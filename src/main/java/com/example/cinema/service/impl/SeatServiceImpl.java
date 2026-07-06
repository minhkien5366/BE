package com.example.cinema.service.impl;

import com.example.cinema.dto.SeatRequest;
import com.example.cinema.entity.*;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.*;
import com.example.cinema.service.SeatService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SeatPriceConfigRepository seatPriceConfigRepository;
    
    // ================= REDIS INJECTION =================
    private final SeatLockRepository seatLockRepository; 

    // ================= PRICE CONFIG =================
    private static final double PRICE_NORMAL = 80000.0;
    private static final double PRICE_VIP = 120000.0;
    private static final double PRICE_SWEETBOX = 250000.0;

    // ================= LOGIC KIỂM TRA SUẤT CHIẾU TƯƠNG LAI =================
    private boolean hasActiveOrFutureShowtimes(Long roomId) {
        List<Showtime> showtimes = showtimeRepository.findByRoomId(roomId);
        if (showtimes == null || showtimes.isEmpty()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        for (Showtime st : showtimes) {
            if (st.getStartTime() == null) continue;

            int durationMinutes = 150; 
            try {
                if (st.getMovie() != null && st.getMovie().getDuration() != null) {
                    durationMinutes = st.getMovie().getDuration();
                }
            } catch (Exception e) {}

            LocalDateTime endTime = st.getStartTime().plusMinutes(durationMinutes);

            if (now.isBefore(endTime)) {
                return true; 
            }
        }
        return false; 
    }

    // ================= SHOWTIME & AVAILABILITY =================
@Override
    public List<Seat> getSeatsByShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));

        if (showtime.getRoom() == null) return new ArrayList<>();

        List<Seat> seats = seatRepository.findByRoomId(showtime.getRoom().getId());

        List<Ticket> tickets = ticketRepository.findByShowtimeId(showtimeId);
        
        // 🔥 Gộp tất cả các trạng thái vào một Set
        Set<Long> occupied = tickets.stream()
                .filter(t -> t.getSeat() != null)
                .filter(t -> {
                    String s = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
                    return s.equals("BOOKED") || s.equals("PAID") || s.equals("USED");
                })
                .map(t -> t.getSeat().getId())
                .collect(Collectors.toSet());

        // 🔥 Lấy kho ảnh nhân vật từ Phim
        List<String> charImages = new ArrayList<>();
        if (showtime.getMovie() != null && showtime.getMovie().getCharacterImages() != null) {
            String imagesStr = showtime.getMovie().getCharacterImages().trim();
            if (!imagesStr.isEmpty()) {
                charImages = Arrays.asList(imagesStr.split(","));
            }
        }

        int javaDay = showtime.getStartTime().getDayOfWeek().getValue();
        int dayValue = (javaDay == 7) ? 8 : javaDay + 1;
        List<SeatPriceConfig> allPrices = seatPriceConfigRepository.findAll();

        // Xử lý an toàn email người dùng hiện tại (Tránh lỗi văng app khi khách vãng lai xem ghế)
        String currentUserEmail = "anonymousUser";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        // =========================================================================
        // 🔥 TỐI ƯU HIỆU NĂNG REDIS UPSTASH ĐA LUỒNG (CHỐNG LAG N+1 TRIỆT ĐỂ)
        // =========================================================================
        
        // 1. Gom tất cả ID ghế thành 1 mảng danh sách Key
        List<String> allRedisKeys = seats.stream()
                .map(s -> showtimeId + "_" + s.getId())
                .collect(Collectors.toList());

        // 2. 🔥 VŨ KHÍ HẠNG NẶNG: Ép CPU phân chia nhiều luồng bắn request cùng một lúc (Parallel Stream)
        List<SeatLock> activeLocks = allRedisKeys.parallelStream()
                .map(key -> seatLockRepository.findById(key).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3. Đổ dữ liệu vào Map để tra cứu siêu tốc trong RAM (Big O(1))
        Map<String, SeatLock> lockMap = new HashMap<>();
        for (SeatLock lock : activeLocks) {
            if (lock != null && lock.getId() != null) {
                lockMap.put(lock.getId(), lock);
            }
        }
        // =========================================================================

        for (Seat s : seats) {
            String redisKey = showtimeId + "_" + s.getId();
            
            // 4. Móc trạng thái khóa từ trong RAM ra, KHÔNG GỌI LÊN MẠNG NỮA
            SeatLock lock = lockMap.get(redisKey);
            
            boolean isLockedByOther = lock != null && !lock.getUserId().equals(currentUserEmail);

            if (occupied.contains(s.getId()) || isLockedByOther) {
                 s.setStatus("OCCUPIED");
                 
                 // 🔥 Gán ảnh nhân vật ngẫu nhiên dựa trên tên ghế (để cố định ảnh không bị nhảy)
                 if (!charImages.isEmpty()) {
                     int index = Math.abs(s.getName().hashCode()) % charImages.size();
                     s.setUserAvatar(charImages.get(index).trim());
                 }
            } else {
                 s.setStatus("AVAILABLE");
                 s.setUserAvatar(null); // Ghế trống không hiển thị ảnh
            }

            Double dynamicPrice = allPrices.stream()
                    .filter(p -> p.getSeatType().equalsIgnoreCase(s.getSeatType()) && p.getDayOfWeek() == dayValue)
                    .map(SeatPriceConfig::getPrice)
                    .findFirst()
                    .orElse(s.getPrice());

            s.setPrice(dynamicPrice);
        }

        return seats;
    }
    // ================= VALIDATE GHẾ =================
    @Override
    public void validateSeatSelection(
            Long showtimeId,
            List<Long> selectedSeatIds
    ) {
        User currentUser = getCurrentUser();
        for (Long seatId : selectedSeatIds) {
            String redisKey = showtimeId + "_" + seatId;
            Optional<SeatLock> lock = seatLockRepository.findById(redisKey);
            
            if (lock.isPresent()) {
                if (!lock.get().getUserId().equals(currentUser.getEmail())) {
                    throw new RuntimeException("Rất tiếc! Một vài ghế bạn chọn vừa có người khác nhanh tay giữ chỗ. Vui lòng chọn ghế khác!");
                }
            }
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));

        List<Seat> allSeats = seatRepository.findByRoomId(showtime.getRoom().getId());

        List<Ticket> tickets = ticketRepository.findByShowtimeId(showtimeId);

        Set<Long> alreadyOccupied = tickets.stream()
                .filter(t -> {
                    String status = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
                    return status.equals("BOOKED") || status.equals("PAID") || status.equals("USED");
                })
                .filter(t -> t.getSeat() != null)
                .map(t -> t.getSeat().getId())
                .collect(Collectors.toSet());

        Set<Long> newlySelected = new HashSet<>(selectedSeatIds);

        Map<String, List<Seat>> seatsByRow = allSeats.stream()
                .collect(Collectors.groupingBy(Seat::getSeatRow));

        for (Map.Entry<String, List<Seat>> entry : seatsByRow.entrySet()) {

            List<Seat> rowSeats = entry.getValue();

            Map<Integer, Seat> seatMapByNum = new HashMap<>();

            for (Seat s : rowSeats) {
                try {
                    seatMapByNum.put(Integer.parseInt(s.getSeatNumber()), s);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            for (Seat currentSeat : rowSeats) {

                String seatType = currentSeat.getSeatType() != null
                                ? currentSeat.getSeatType().toUpperCase()
                                : "NORMAL";

                if ("SWEETBOX".equals(seatType) || "COUPLE".equals(seatType)) {
                    continue;
                }

                Long currentId = currentSeat.getId();

                boolean isOccupied = alreadyOccupied.contains(currentId);
                boolean isSelected = newlySelected.contains(currentId);

                if (!isOccupied && !isSelected) {

                    int currentNum = Integer.parseInt(currentSeat.getSeatNumber());

                    // LEFT
                    Seat leftSeat = seatMapByNum.get(currentNum - 1);
                    boolean leftBlocked = false;
                    boolean leftSelected = false;

                    if (leftSeat != null) {
                        boolean leftOccupied = alreadyOccupied.contains(leftSeat.getId());
                        boolean leftSimSelected = newlySelected.contains(leftSeat.getId());
                        if (leftOccupied || leftSimSelected) {
                            leftBlocked = true;
                            if (leftSimSelected) leftSelected = true;
                        }
                    }

                    // RIGHT
                    Seat rightSeat = seatMapByNum.get(currentNum + 1);
                    boolean rightBlocked = false;
                    boolean rightSelected = false;

                    if (rightSeat != null) {
                        boolean rightOccupied = alreadyOccupied.contains(rightSeat.getId());
                        boolean rightSimSelected = newlySelected.contains(rightSeat.getId());
                        if (rightOccupied || rightSimSelected) {
                            rightBlocked = true;
                            if (rightSimSelected) rightSelected = true;
                        }
                    }

                    if (leftBlocked && rightBlocked) {
                        if (leftSelected || rightSelected) {
                            throw new RuntimeException(
                                    "Không được để lại ghế trống đơn lẻ ("
                                            + currentSeat.getName()
                                            + ") ở giữa hàng ghế!"
                            );
                        }
                    }
                }
            }
        }
    }

    // ================= AUTO GENERATE =================
    @Override
    @Transactional
    public List<Seat> generateSeatsForRoom(
            Long roomId,
            int numRows,
            int seatsPerRow
    ) {
        validateRoomAccess(roomId);

        if (hasActiveOrFutureShowtimes(roomId)) {
            throw new RuntimeException(
                    "Phòng đang có suất chiếu hoạt động hoặc sắp diễn ra, không thể chỉnh sửa sơ đồ ghế!"
            );
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));

        int total = numRows * seatsPerRow;

        if (total > room.getTotalSeats()) {
            throw new RuntimeException("Vượt quá sức chứa phòng");
        }

        seatRepository.deleteByRoomId(roomId);

        List<Seat> seats = new ArrayList<>();

        char row = 'A';

        for (int i = 0; i < numRows; i++) {
            for (int j = 1; j <= seatsPerRow; j++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setSeatRow(String.valueOf(row));
                seat.setSeatNumber(String.valueOf(j));
                seat.setName(row + String.valueOf(j));
                seat.setStatus("AVAILABLE");
                seat.setSeatType("NORMAL");
                seat.setPrice(PRICE_NORMAL);
                seats.add(seat);
            }
            row++;
        }

        return seatRepository.saveAll(seats);
    }

    // ================= CREATE =================
    @Override
    @Transactional
    public Seat createSeat(SeatRequest request) {
        validateRoomAccess(request.getRoomId());

        if (hasActiveOrFutureShowtimes(request.getRoomId())) {
            throw new RuntimeException(
                    "Phòng đang có suất chiếu hoạt động hoặc sắp diễn ra, không thể thêm ghế!"
            );
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));

        long count = seatRepository.countByRoomId(room.getId());

        if (count >= room.getTotalSeats()) {
            throw new RuntimeException("Phòng đã đầy, không thể thêm ghế mới");
        }

        List<Seat> existingSeats = seatRepository.findByRoomId(room.getId());

        boolean isDuplicate = existingSeats.stream()
                .anyMatch(s -> s.getSeatRow().equalsIgnoreCase(request.getSeatRow())
                            && s.getSeatNumber().equalsIgnoreCase(String.valueOf(request.getSeatNumber()))
                );

        if (isDuplicate) {
            throw new RuntimeException(
                    "Vị trí ghế " + request.getSeatRow() + request.getSeatNumber() + " đã tồn tại!"
            );
        }

        Seat seat = new Seat();
        seat.setRoom(room);
        seat.setStatus("AVAILABLE");
        mapRequestToEntity(request, seat, room);
        validateSweetboxPairing(room.getId(), seat, false);

        return seatRepository.save(seat);
    }

    // ================= UPDATE =================
    @Override
    @Transactional
    public Seat updateSeat(Long id, SeatRequest request) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghế"));

        validateRoomAccess(seat.getRoom().getId());

        if (hasActiveOrFutureShowtimes(seat.getRoom().getId())) {
            throw new RuntimeException(
                    "Phòng đang có suất chiếu hoạt động hoặc sắp diễn ra, không thể cập nhật ghế!"
            );
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));

        List<Seat> existingSeats = seatRepository.findByRoomId(room.getId());

        boolean isDuplicate = existingSeats.stream()
                .anyMatch(s -> !s.getId().equals(id)
                            && s.getSeatRow().equalsIgnoreCase(request.getSeatRow())
                            && s.getSeatNumber().equalsIgnoreCase(String.valueOf(request.getSeatNumber()))
                );

        if (isDuplicate) {
            throw new RuntimeException("Vị trí ghế cập nhật đã bị trùng!");
        }

        mapRequestToEntity(request, seat, room);

        if (request.getStatus() != null) {
            seat.setStatus(request.getStatus());
        }

        validateSweetboxPairing(room.getId(), seat, true);

        return seatRepository.save(seat);
    }

    // ================= SWEETBOX =================
    private void validateSweetboxPairing(
            Long roomId,
            Seat targetSeat,
            boolean isUpdate
    ) {
        if (!"SWEETBOX".equalsIgnoreCase(targetSeat.getSeatType())) {
            return;
        }

        int seatNum = Integer.parseInt(targetSeat.getSeatNumber());
        int partnerNumber = (seatNum % 2 != 0) ? (seatNum + 1) : (seatNum - 1);

        List<Seat> existingSeats = seatRepository.findByRoomId(roomId);

        boolean hasPartner = existingSeats.stream()
                .anyMatch(s -> s.getSeatRow().equalsIgnoreCase(targetSeat.getSeatRow())
                            && s.getSeatNumber().equals(String.valueOf(partnerNumber))
                            && "SWEETBOX".equalsIgnoreCase(s.getSeatType())
                );

        if (!hasPartner && !isUpdate) {
            System.out.println("[Cảnh báo] Ghế Sweetbox " + targetSeat.getName() + " đang thiếu cặp!");
        }
    }

    // ================= DELETE =================
    @Override
    @Transactional
    public void deleteSeat(Long id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ghế không tồn tại"));

        validateRoomAccess(seat.getRoom().getId());

        if (hasActiveOrFutureShowtimes(seat.getRoom().getId())) {
            throw new RuntimeException(
                    "Phòng đang có suất chiếu hoạt động hoặc sắp diễn ra, không thể xóa ghế!"
            );
        }

        seatRepository.deleteById(id);
    }

    // ================= ROOM MANAGEMENT =================
    @Override
    public List<Seat> getSeatsByRoom(Long roomId) {
        validateRoomAccess(roomId);
        return seatRepository.findByRoomId(roomId);
    }

    @Override
    public List<Seat> getAllSeats() {
        User user = getCurrentUser();

        if (isSuperAdmin(user)) {
            return seatRepository.findAll();
        }

        return seatRepository.findByRoom_CinemaItem_Id(user.getManagedCinemaItemId());
    }

    @Override
    @Transactional
    public void deleteSeatsByRoom(Long roomId) {
        validateRoomAccess(roomId);

        if (hasActiveOrFutureShowtimes(roomId)) {
            throw new RuntimeException(
                    "Phòng đang có suất chiếu hoạt động hoặc sắp diễn ra, không thể xóa toàn bộ ghế!"
            );
        }

        seatRepository.deleteByRoomId(roomId);
    }

    @Override
    public Map<String, Boolean> checkSeatEligibility(Long id) {
        Seat seat = seatRepository.findById(id).orElse(null);
        boolean canDelete = true;

        if (seat != null && seat.getRoom() != null) {
            if (hasActiveOrFutureShowtimes(seat.getRoom().getId())) {
                canDelete = false;
            }
        } else {
            canDelete = false;
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("canDelete", canDelete);

        return response;
    }

    // ================= MAPPER =================
    private void mapRequestToEntity(
            SeatRequest request,
            Seat seat,
            Room room
    ) {
        seat.setSeatRow(request.getSeatRow());
        seat.setSeatNumber(String.valueOf(request.getSeatNumber()));
        seat.setName(request.getSeatRow() + request.getSeatNumber());
        seat.setRoom(room);

        String type = request.getSeatType();
        if (type == null) {
            type = "NORMAL";
        }
        type = type.toUpperCase();
        seat.setSeatType(type);

        switch (type) {
            case "VIP":
                seat.setPrice(PRICE_VIP);
                break;
            case "SWEETBOX":
                seat.setPrice(PRICE_SWEETBOX);
                break;
            default:
                seat.setPrice(PRICE_NORMAL);
        }
    }

    // ================= SECURITY =================
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Phiên đăng nhập không tồn tại hoặc hết hạn"));
    }

    private boolean isSuperAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getRoleName().toUpperCase().contains("ADMIN"));
    }

    private void validateRoomAccess(Long roomId) {
        User user = getCurrentUser();

        if (isSuperAdmin(user)) {
            return;
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));

        if (room.getCinemaItem() == null || !room.getCinemaItem().getId().equals(user.getManagedCinemaItemId())) {
            throw new RuntimeException("Bạn không có quyền quản lý phòng này!");
        }
    }

    // ================= REAL-TIME LOCKING LOGIC =================
    @Override
    public void holdSeat(Long showtimeId, Long seatId, String userEmail) {
        String redisKey = showtimeId + "_" + seatId;
        Optional<SeatLock> lockOpt = seatLockRepository.findById(redisKey);
        
        if (lockOpt.isPresent()) {
            if (!lockOpt.get().getUserId().equals(userEmail)) {
                throw new RuntimeException("Chậm tay rồi! Ghế này vừa bị người khác giữ.");
            }
        } else {
            // Khóa 10 phút (600 giây) ngay khi click chạm vào ghế
            seatLockRepository.save(new SeatLock(redisKey, userEmail, 600L));
        }
    }

    @Override
    public void releaseSeat(Long showtimeId, Long seatId, String userEmail) {
        String redisKey = showtimeId + "_" + seatId;
        Optional<SeatLock> lockOpt = seatLockRepository.findById(redisKey);
        
        // Chỉ nhả ghế nếu đúng cái người đang khóa bấm bỏ chọn
        if (lockOpt.isPresent() && lockOpt.get().getUserId().equals(userEmail)) {
            seatLockRepository.deleteById(redisKey);
        }
    }
}
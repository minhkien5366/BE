package com.example.cinema.service.impl;

import com.example.cinema.dto.ShowtimeRequest;
import com.example.cinema.entity.*;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.*;
import com.example.cinema.service.MailService;
import com.example.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final CinemaItemRepository cinemaItemRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository; 
    private final PointHistoryRepository pointHistoryRepository;
    private final MailService mailService; // 🔥 Dùng để gọi form gửi mail xin lỗi hủy vé

    private String readStringCell(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return null;
    }

    private LocalDateTime readDateCell(Cell cell, DateTimeFormatter formatter) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }
        return LocalDateTime.parse(cell.getStringCellValue().trim(), formatter);
    }

    @Override
    public List<Showtime> getAll() {
        User user = getCurrentUser();
        if (isSuperAdmin(user)) {
            return showtimeRepository.findAll();
        } else {
            return showtimeRepository.findByCinemaItem_Id(user.getManagedCinemaItemId());
        }
    }

    @Override
    public Showtime getById(Long id) {
        return showtimeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Suất chiếu không tồn tại"));
    }

    @Override
    public List<Showtime> getByMovie(Long movieId) { 
        return showtimeRepository.findByMovieId(movieId); 
    }

    @Override
    public List<Showtime> getByCinemaItem(Long cinemaItemId) { 
        return showtimeRepository.findByCinemaItem_Id(cinemaItemId); 
    }

    @Override
    @Transactional
    public void importExcel(MultipartFile file) {
        User user = getCurrentUser();
        Long managedCinemaId = user.getManagedCinemaItemId();
        if (managedCinemaId == null) {
            throw new RuntimeException("Tài khoản của bạn không được phân quyền quản lý chi nhánh nào!");
        }

        try (InputStream is = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String movieName = readStringCell(row.getCell(0));
                    String roomName = readStringCell(row.getCell(1));
                    LocalDateTime startTime = readDateCell(row.getCell(2), formatter);
                    
                    if (movieName == null || roomName == null || startTime == null) {
                        throw new RuntimeException("Thiếu dữ liệu bắt buộc");
                    }
                    if (startTime.isBefore(LocalDateTime.now())) {
                        throw new RuntimeException("Thời gian bắt đầu suất chiếu không được ở quá khứ");
                    }
                    
                    Movie movie = movieRepository.findFirstByTitleIgnoreCase(movieName)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy phim: " + movieName));
                            
                    Room room = roomRepository.findByNameAndCinemaItem_Id(roomName, managedCinemaId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng '" + roomName + "' thuộc chi nhánh của bạn"));
                    CinemaItem cinemaItem = room.getCinemaItem();
                    
                    validateBranchAccess(cinemaItem.getId());
                    LocalDateTime endTime = startTime.plusMinutes(movie.getDuration());
                    
                    checkShowtimeOverlapWithBuffer(room.getId(), startTime, endTime, null);
                    
                    boolean isMovieOverlapping = showtimeRepository.findByCinemaItem_Id(managedCinemaId).stream()
                            .filter(s -> "ACTIVE".equals(s.getStatus()) || "PENDING_CANCEL".equals(s.getStatus()))
                            .filter(s -> s.getMovie().getId().equals(movie.getId())) // Cùng 1 phim
                            .anyMatch(s -> {
                                return startTime.isBefore(s.getEndTime()) && endTime.isAfter(s.getStartTime());
                            });

                    if (isMovieOverlapping) {
                        throw new RuntimeException("Phim '" + movie.getTitle() + "' đã có lịch chiếu ở một phòng khác trong khung giờ này (" 
                                + startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " 
                                + endTime.format(DateTimeFormatter.ofPattern("HH:mm")) + ")!");
                    }
                    
                    Showtime showtime = Showtime.builder()
                            .movie(movie)
                            .room(room)
                            .cinemaItem(cinemaItem)
                            .startTime(startTime)
                            .endTime(endTime)
                            .status("ACTIVE")
                            .build();
                    showtimeRepository.save(showtime);
                } catch (Exception rowError) {
                    throw new RuntimeException("Lỗi dòng " + (i + 1) + ": " + rowError.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Import thất bại: " + e.getMessage());
        }
    }


    @Override
    @Transactional
    public Showtime createShowtime(ShowtimeRequest request) {
        validateBranchAccess(request.getCinemaItemId());

        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian bắt đầu không thể ở quá khứ!");
        }

        Movie movie = movieRepository.findById(request.getMovieId()).orElseThrow(() -> new ResourceNotFoundException("Phim không tồn tại"));
        Room room = roomRepository.findById(request.getRoomId()).orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));

        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration());
        checkShowtimeOverlapWithBuffer(room.getId(), request.getStartTime(), endTime, null);

        Showtime showtime = new Showtime();
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setMovie(movie);
        showtime.setCinemaItem(cinemaItemRepository.findById(request.getCinemaItemId()).get());
        showtime.setRoom(room);
        showtime.setStatus("ACTIVE");

        return showtimeRepository.save(showtime);
    }

    @Override
    @Transactional
    public Showtime updateShowtime(Long id, ShowtimeRequest request) {
        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Suất chiếu không tồn tại"));
        validateBranchAccess(showtime.getCinemaItem().getId());

        if (ticketRepository.existsByShowtimeIdAndStatus(id, "PAID")) {
            throw new RuntimeException("Lỗi: Không thể SỬA! Đã có khách hàng THANH TOÁN vé cho suất chiếu này.");
        }

        Movie movie = movieRepository.findById(request.getMovieId()).orElseThrow(() -> new ResourceNotFoundException("Phim không tồn tại"));
        Room room = roomRepository.findById(request.getRoomId()).orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));

        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration());
        checkShowtimeOverlapWithBuffer(room.getId(), request.getStartTime(), endTime, id);

        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setMovie(movie);
        showtime.setRoom(room);
        
        return showtimeRepository.save(showtime);
    }

    @Override
    public List<Showtime> getByMovieAndDate(Long movieId, String dateStr) {
        List<Showtime> showtimes = showtimeRepository.findByMovieIdAndDate(movieId, LocalDate.parse(dateStr));
        LocalDateTime now = LocalDateTime.now();
        return showtimes.stream()
                .filter(s -> s.getStartTime().isAfter(now))
                .filter(s -> "ACTIVE".equals(s.getStatus())) 
                .collect(Collectors.toList());
    }

    // ==========================================
    // ADMIN XÓA: BẮT BUỘC QUA SUPER ADMIN NẾU CÓ >= 1 VÉ
    // ==========================================
    @Override
    @Transactional
    public void deleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));
        validateBranchAccess(showtime.getCinemaItem().getId());

        long paidCount = ticketRepository.findByShowtimeId(id).stream().filter(t -> "PAID".equals(t.getStatus())).count();

        if (paidCount > 0) {
            throw new RuntimeException("Lỗi: Đã có khách THANH TOÁN vé! Vui lòng dùng tính năng 'Gửi xin phép hủy' để Super Admin duyệt.");
        }

        showtimeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Showtime requestCancel(Long id, String reason) {
        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Suất chiếu không tồn tại"));
        validateBranchAccess(showtime.getCinemaItem().getId());

        long paidCount = ticketRepository.findByShowtimeId(id).stream().filter(t -> "PAID".equals(t.getStatus())).count();

        if (paidCount == 0) {
            throw new RuntimeException("Suất chiếu này chưa có vé thanh toán, bạn có thể XÓA TRỰC TIẾP mà không cần xin phép!");
        }

        if ("PENDING_CANCEL".equals(showtime.getStatus())) {
            throw new RuntimeException("Suất chiếu này đã được gửi yêu cầu hủy trước đó, đang chờ duyệt!");
        }

        // Bắt buộc chuyển sang trạng thái chờ chờ Super Admin duyệt (không quan tâm là 1 hay 100 vé)
        showtime.setStatus("PENDING_CANCEL");
        showtime.setCancelReason(reason);
        return showtimeRepository.save(showtime);
    }

    // ==========================================
    // SUPER ADMIN: DUYỆT / TỪ CHỐI
    // ==========================================
    @Override
    @Transactional
    public void approveCancel(Long id) {
        User currentUser = getCurrentUser();
        if (!isSuperAdmin(currentUser)) {
            throw new RuntimeException("Lỗi: Chỉ Super Admin mới có quyền duyệt hủy suất chiếu!");
        }

        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Suất chiếu không tồn tại"));

        if (!"PENDING_CANCEL".equals(showtime.getStatus())) {
            throw new RuntimeException("Suất chiếu này không ở trạng thái chờ duyệt hủy!");
        }

        // Thực thi quy trình Hủy - Đền bù - Gửi Mail (isSystemAuto = false vì do người duyệt)
        processCancellationAndRefund(showtime, showtime.getCancelReason(), false);
    }

    @Override
    @Transactional
    public Showtime rejectCancel(Long id) {
        User user = getCurrentUser();
        if (!isSuperAdmin(user)) {
            throw new RuntimeException("Lỗi: Chỉ Super Admin mới có quyền từ chối hủy suất chiếu!");
        }

        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Suất chiếu không tồn tại"));

        showtime.setStatus("ACTIVE");
        showtime.setCancelReason(null);
        return showtimeRepository.save(showtime);
    }

    // ==========================================
    // 🔥 BOT CHẠY NGẦM CỦA HỆ THỐNG: TỰ ĐỘNG HỦY SUẤT CHIẾU VẮNG KHÁCH (DƯỚI 5 VÉ)
    // ==========================================
    @Scheduled(fixedRate = 1800000) // Quét tự động mỗi 30 phút
    @Transactional
    public void autoCancelUnderbookedShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(2); // Chỉ quét các phim sẽ chiếu trong 2 tiếng tới

        List<Showtime> upcomingShowtimes = showtimeRepository.findAll().stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .filter(s -> s.getStartTime().isAfter(now) && s.getStartTime().isBefore(threshold))
                .collect(Collectors.toList());

        for (Showtime st : upcomingShowtimes) {
            long paidCount = ticketRepository.findByShowtimeId(st.getId()).stream()
                    .filter(t -> "PAID".equals(t.getStatus())).count();

            // Nếu CÓ NGƯỜI MUA nhưng số lượng vé bán được DƯỚI 5 GHẾ -> Hệ thống (Bot) sẽ vung đao chém luôn
            if (paidCount > 0 && paidCount < 5) {
                processCancellationAndRefund(st, "Hệ thống tự động hủy do số lượng vé bán ra không đạt mức tối thiểu (dưới 5 ghế)", true);
            }
        }
    }

    // ==========================================
    // 🔥 LÕI XỬ LÝ CHUNG: HỦY VÉ, HOÀN ĐIỂM, GỬI EMAIL
    // ==========================================
private void processCancellationAndRefund(Showtime showtime, String reason, boolean isSystemAuto) {
        List<Ticket> tickets = ticketRepository.findByShowtimeId(showtime.getId());
        Set<User> affectedUsers = new HashSet<>();
        
        for (Ticket t : tickets) {
            t.setStatus("CANCELLED");
            
            List<Order> relatedOrders = orderRepository.findAll().stream()
                .filter(order -> "PAID".equals(order.getStatus())) 
                .filter(order -> order.getOrderDetails() != null && order.getOrderDetails().stream()
                    .anyMatch(detail -> "TICKET".equals(detail.getItemType()) && t.getSeat().getId().equals(detail.getItemId())))
                .collect(Collectors.toList());

            for (Order order : relatedOrders) {
                User buyer = order.getUser();
                if (buyer != null && !affectedUsers.contains(buyer)) {
                    // Đền bù điểm: 10.000 VNĐ = 10 điểm -> (1.000 VNĐ = 1 điểm) -> 1 điểm tương đương 1.000 VNĐ cho lần mua sau
                    int compensationPoints = (int) (order.getTotalAmount() / 1000); 
                    buyer.setPoints(buyer.getPoints() + compensationPoints);
                    userRepository.save(buyer);
                    
                    // ================= TÍCH HỢP LƯU LỊCH SỬ CỘNG ĐIỂM =================
                    String movieTitle = (showtime.getMovie() != null) ? showtime.getMovie().getTitle() : "ID " + showtime.getId();
                    PointHistory history = PointHistory.builder()
                            .user(buyer)
                            .amount(compensationPoints)
                            .description("Hoàn điểm do hủy lịch chiếu: " + movieTitle)
                            .type("EARNED")
                            .build();
                    pointHistoryRepository.save(history);
                    // =================================================================
                    
                    affectedUsers.add(buyer); 
                    
                    // Gửi Email Tự Động xin lỗi khách (Sử dụng hàm mới của MailService)
                    mailService.sendShowtimeCancellationEmail(buyer, showtime, compensationPoints, isSystemAuto);
                }
                
                order.setStatus("CANCELLED");
                orderRepository.save(order);
            }
        }
        
        ticketRepository.saveAll(tickets);

        showtime.setStatus("CANCELLED");
        showtime.setCancelReason(reason);
        showtimeRepository.save(showtime);
    }
    private void checkShowtimeOverlapWithBuffer(Long roomId, LocalDateTime newStart, LocalDateTime newEnd, Long excludeId) {
        List<Showtime> existing = showtimeRepository.findByRoomId(roomId);
        int buffer = 20;

        for (Showtime s : existing) {
            if ("CANCELLED".equals(s.getStatus())) continue; 
            if (excludeId != null && s.getId().equals(excludeId)) continue;

            LocalDateTime sStartBuffer = s.getStartTime().minusMinutes(buffer);
            LocalDateTime sEndBuffer = s.getEndTime().plusMinutes(buffer);

            if (newStart.isBefore(sEndBuffer) && newEnd.isAfter(sStartBuffer)) {
                throw new RuntimeException("Trùng lịch hoặc quá sát suất chiếu khác! Đang có suất chiếu từ " 
                        + s.getStartTime().toLocalTime() + " đến " + s.getEndTime().toLocalTime());
            }
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    private boolean isSuperAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getRoleName().equalsIgnoreCase("SUPER_ADMIN") || r.getRoleName().equalsIgnoreCase("ROLE_SUPER_ADMIN"));
    }

    private void validateBranchAccess(Long cinemaItemId) {
        User user = getCurrentUser();
        if (isSuperAdmin(user)) return;
        if (user.getManagedCinemaItemId() == null || !user.getManagedCinemaItemId().equals(cinemaItemId)) {
            throw new RuntimeException("Bạn không có quyền quản lý tại chi nhánh rạp này!");
        }
    }
}
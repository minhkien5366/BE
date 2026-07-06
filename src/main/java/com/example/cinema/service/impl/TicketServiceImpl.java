package com.example.cinema.service.impl;

import com.example.cinema.dto.TicketRequest;
import com.example.cinema.entity.*;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.*;
import com.example.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;

@Override
@Transactional
public Ticket createTicket(TicketRequest request) {

    Seat seat = seatRepository.findById(request.getSeatId())
            .orElseThrow(() -> new ResourceNotFoundException("Ghế không tồn tại"));

    Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
            .orElseThrow(() -> new ResourceNotFoundException("Suất chiếu không tồn tại"));

    // check ghế đã đặt chưa
    List<String> busyStatuses = Arrays.asList("BOOKED", "PAID");

    if (ticketRepository.existsBySeatAndShowtimeAndStatusIn(seat, showtime, busyStatuses)) {
        throw new RuntimeException("Ghế " + seat.getName() + " đã có người đặt!");
    }

    User user = getCurrentUser();

    Ticket ticket = new Ticket();

    // ====== RELATION ======
    ticket.setSeat(seat);
    ticket.setShowtime(showtime);
    ticket.setUser(user);

    // ====== SNAPSHOT GHẾ (QUAN TRỌNG) ======
    ticket.setSeatRow(seat.getSeatRow());
    ticket.setSeatNumber(seat.getSeatNumber());
    ticket.setSeatName(seat.getName());

    // ====== DATA ======
    ticket.setPrice(seat.getPrice());
    ticket.setStatus("BOOKED");

    String bookingCode = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 8)
            .toUpperCase();

    ticket.setBookingCode(bookingCode);

    return ticketRepository.save(ticket);
}
    @Override
    public List<Ticket> getMyTickets() {
        return ticketRepository.findByUserEmailOrderByCreatedAtDesc(getCurrentUser().getEmail());
    }

    @Override
    public Ticket getByBookingCode(String code) {
        Ticket ticket = ticketRepository.findByBookingCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vé với mã: " + code));
        
        // Nếu là Admin gọi hàm này, phải check xem vé có thuộc rạp của họ không
        validateAdminAccess(ticket);
        return ticket;
    }

    @Override
    public List<Ticket> getTicketsByShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Suất chiếu không tồn tại"));
        
        // Kiểm tra xem Admin có quyền quản lý rạp chứa suất chiếu này không
        validateBranchId(showtime.getCinemaItem().getId());
        
        return ticketRepository.findByShowtimeId(showtimeId);
    }

    @Override
    public List<Ticket> getAllTickets() {
        User user = getCurrentUser();
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        
        if (isSuperAdmin(user)) {
            return ticketRepository.findAll(sort);
        }
        
        // Admin thường: Chỉ lấy vé của chi nhánh mình quản lý
        return ticketRepository.findByShowtime_CinemaItem_Id(user.getManagedCinemaItemId(), sort);
    }

    @Override
    @Transactional
    public void cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vé"));
        
        validateAdminAccess(ticket);

        if ("PAID".equals(ticket.getStatus())) {
            throw new RuntimeException("Vé đã thanh toán. Vui lòng thực hiện quy trình hoàn tiền tại quầy.");
        }
        
        ticket.setStatus("CANCELLED");
        ticketRepository.save(ticket);
    }

    // --- HELPER METHODS BẢO MẬT ---

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Phiên đăng nhập hết hạn"));
    }

    private boolean isSuperAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getRoleName().equals("SUPER_ADMIN"));
    }

    private void validateBranchId(Long cinemaItemId) {
        User user = getCurrentUser();
        if (isSuperAdmin(user)) return;
        if (user.getManagedCinemaItemId() == null || !user.getManagedCinemaItemId().equals(cinemaItemId)) {
            throw new RuntimeException("Bạn không có quyền quản lý vé tại chi nhánh rạp này!");
        }
    }

    private void validateAdminAccess(Ticket ticket) {
        User user = getCurrentUser();
        // Super Admin thấy hết, User xem vé của chính mình, Admin xem vé rạp mình
        if (isSuperAdmin(user)) return;
        
        boolean isOwner = ticket.getUser().getEmail().equals(user.getEmail());
        boolean isBranchAdmin = user.getManagedCinemaItemId() != null 
                && user.getManagedCinemaItemId().equals(ticket.getShowtime().getCinemaItem().getId());

        if (!isOwner && !isBranchAdmin) {
            throw new RuntimeException("Bạn không có quyền truy cập thông tin vé này!");
        }
    }
}
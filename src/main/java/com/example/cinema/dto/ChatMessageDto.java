package com.example.cinema.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatMessageDto {
    private String roomId;      // ID phòng (ví dụ: Tên khách hàng hoặc Session ID)
    private String sender;      // Tên người gửi ("Khách Hàng", "A&K AI", "Admin")
    private String content;     // Nội dung tin nhắn
    private String senderRole;  // Vai trò người gửi: "USER", "BOT", "ADMIN"
    private String receiverRole;// Vai trò người nhận: "BOT", "ADMIN"
    private String timestamp;   // Giờ gửi tin
    
    // Mã chi nhánh rạp để định tuyến tin nhắn chuẩn xác về đúng rạp
    private Long cinemaItemId;  

    // Constructor dùng riêng cho việc khởi tạo tin nhắn BOT
    public ChatMessageDto(String roomId, String sender, String content, String senderRole, String receiverRole, String timestamp) {
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
        this.senderRole = senderRole;
        this.receiverRole = receiverRole;
        this.timestamp = timestamp;
    }

    // 🔥 VÁ LỖI VS CODE: Viết tay luôn hàm Getter/Setter cho nó nhận diện ngay lập tức
    public Long getCinemaItemId() {
        return cinemaItemId;
    }

    public void setCinemaItemId(Long cinemaItemId) {
        this.cinemaItemId = cinemaItemId;
    }
}
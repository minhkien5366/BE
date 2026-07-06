package com.example.cinema.controller;

import com.example.cinema.dto.ChatMessageDto;
import com.example.cinema.service.impl.AiChatServiceImpl;
import com.example.cinema.service.impl.ChatMemoryManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMemoryManager chatMemoryManager;
    private final AiChatServiceImpl aiChatService;

    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessageDto message) {
        
        // 🔥 VÁ LỖI ADMIN HIỂN THỊ "ĐÃ ĐÓNG": 
        // Khi khách hàng gọi lại Admin, lập tức xóa sạch mọi lệnh [SYSTEM_CLOSE] cũ trong RAM
        if ("ADMIN".equals(message.getReceiverRole()) && "USER".equals(message.getSenderRole())) {
            try {
                List<ChatMessageDto> history = chatMemoryManager.getRoomHistory(message.getRoomId());
                if (history != null) {
                    history.removeIf(m -> "[SYSTEM_CLOSE]".equals(m.getContent()));
                }
            } catch (Exception ignored) { }
            
            // Bắn một lệnh ngầm để gỡ cờ "đóng phiên" đang kẹt trên màn hình Admin/User
            // SỬA LỖI: Dùng setter thay vì Constructor
            ChatMessageDto reopenNotice = new ChatMessageDto();
            reopenNotice.setRoomId(message.getRoomId());
            reopenNotice.setSender("HỆ THỐNG");
            reopenNotice.setContent("[SYSTEM_OPEN]");
            reopenNotice.setSenderRole("SYSTEM");
            reopenNotice.setReceiverRole("ADMIN");
            reopenNotice.setCinemaItemId(message.getCinemaItemId());

            messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), reopenNotice);
        }

        // Lưu tin nhắn của khách/Admin vào RAM
        chatMemoryManager.saveMessage(message);
        
        // Phát tin nhắn này vào phòng chat
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);

        // PHÂN LUỒNG 1: Nếu khách đang ép chat với AI (BOT)
        if ("BOT".equals(message.getReceiverRole())) {
            
            List<ChatMessageDto> history = chatMemoryManager.getRoomHistory(message.getRoomId());
            String botReplyContent = aiChatService.getAiResponse(message.getContent(), history);
            
            // Đóng gói tin nhắn của AI - Dùng setter để tránh lỗi Constructor
            ChatMessageDto botReply = new ChatMessageDto();
            botReply.setRoomId(message.getRoomId());
            botReply.setSender("A&K AI");
            botReply.setContent(botReplyContent);
            botReply.setSenderRole("BOT");
            botReply.setReceiverRole("USER");
            botReply.setCinemaItemId(null);
            
            chatMemoryManager.saveMessage(botReply);
            messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), botReply);
            
        } 
        // PHÂN LUỒNG 2: Định tuyến gặp người thật theo đúng Chi nhánh
        else if ("ADMIN".equals(message.getReceiverRole())) {
            if (message.getCinemaItemId() != null) {
                messagingTemplate.convertAndSend("/topic/admin.notifications.cinema." + message.getCinemaItemId(), message);
            } else {
                messagingTemplate.convertAndSend("/topic/admin.notifications.general", message);
            }
        }
    }

    @GetMapping("/api/v1/chat/history/{roomId}")
    @ResponseBody
    public List<ChatMessageDto> getChatHistory(@PathVariable String roomId) {
        List<ChatMessageDto> rawHistory = chatMemoryManager.getRoomHistory(roomId);
        if (rawHistory == null || rawHistory.isEmpty()) return rawHistory;

        // LỌC THÔNG MINH CHO TÍNH NĂNG LOAD LẠI TRANG (F5)
        boolean hasAdminReopen = false;
        for (int i = rawHistory.size() - 1; i >= 0; i--) {
            ChatMessageDto msg = rawHistory.get(i);
            if ("ADMIN".equals(msg.getReceiverRole()) || "ADMIN".equals(msg.getSenderRole())) {
                hasAdminReopen = true;
                break;
            } else if ("[SYSTEM_CLOSE]".equals(msg.getContent())) {
                break;
            }
        }

        if (hasAdminReopen) {
            return rawHistory.stream()
                    .filter(m -> !"[SYSTEM_CLOSE]".equals(m.getContent()))
                    .collect(Collectors.toList());
        }

        return rawHistory;
    }

    @GetMapping("/api/v1/chat/active-rooms/{cinemaItemId}")
    @ResponseBody
    public List<String> getActiveRoomsByCinema(@PathVariable Long cinemaItemId) {
        return chatMemoryManager.getAllRooms().stream()
            .filter(roomId -> {
                List<ChatMessageDto> history = chatMemoryManager.getRoomHistory(roomId);
                if (history == null || history.isEmpty()) return false;
                
                boolean matchCinema = history.stream()
                    .anyMatch(m -> cinemaItemId.equals(m.getCinemaItemId()));
                
                boolean isClosed = false;
                for (int i = history.size() - 1; i >= 0; i--) {
                    ChatMessageDto msg = history.get(i);
                    if ("[SYSTEM_CLOSE]".equals(msg.getContent())) {
                        isClosed = true;
                        break;
                    } else if ("ADMIN".equals(msg.getReceiverRole()) || "ADMIN".equals(msg.getSenderRole())) {
                        isClosed = false;
                        break;
                    }
                }
                
                return matchCinema && !isClosed;
            })
            .collect(Collectors.toList());
    }

    @PostMapping("/api/v1/chat/close/{roomId}")
    @ResponseBody
    public String closeChatRoom(@PathVariable String roomId) {
        // 1. Gửi lệnh ngầm báo cho hệ thống Admin biết
        ChatMessageDto closeNotice = new ChatMessageDto();
        closeNotice.setRoomId(roomId);
        closeNotice.setSender("HỆ THỐNG");
        closeNotice.setContent("[SYSTEM_CLOSE]");
        closeNotice.setSenderRole("SYSTEM");
        closeNotice.setReceiverRole("USER");
        closeNotice.setCinemaItemId(null);

        chatMemoryManager.saveMessage(closeNotice);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, closeNotice);

        // 2. Gửi tin nhắn từ AI thay thế chỗ Admin
        ChatMessageDto aiWelcomeBack = new ChatMessageDto();
        aiWelcomeBack.setRoomId(roomId);
        aiWelcomeBack.setSender("A&K AI");
        aiWelcomeBack.setContent("Quản lý đã rời khỏi cuộc trò chuyện. A&K AI đã quay trở lại để tiếp tục hỗ trợ bạn 24/7! Bạn cần mình giúp thêm gì không ạ?");
        aiWelcomeBack.setSenderRole("BOT");
        aiWelcomeBack.setReceiverRole("USER");
        aiWelcomeBack.setCinemaItemId(null);

        chatMemoryManager.saveMessage(aiWelcomeBack);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, aiWelcomeBack);

        return "SUCCESS";
    }
}
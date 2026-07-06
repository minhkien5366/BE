package com.example.cinema.service.impl;

import com.example.cinema.dto.ChatMessageDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatMemoryManager {
    
    // Lưu lịch sử chat trên RAM. Key: roomId, Value: List các tin nhắn
    private final Map<String, List<ChatMessageDto>> chatHistory = new ConcurrentHashMap<>();

    public void saveMessage(ChatMessageDto message) {
        // Tự động gán giờ nếu chưa có
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM")));
        }
        
        chatHistory.computeIfAbsent(message.getRoomId(), k -> new ArrayList<>());
        List<ChatMessageDto> roomLog = chatHistory.get(message.getRoomId());
        
        roomLog.add(message);
        
        // Chống tràn RAM: Chỉ giữ tối đa 50 tin nhắn mới nhất mỗi phòng
        if (roomLog.size() > 50) {
            roomLog.remove(0);
        }
    }

    public List<ChatMessageDto> getRoomHistory(String roomId) {
        return chatHistory.getOrDefault(roomId, new ArrayList<>());
    }

    // 🔥 ĐÃ THÊM: Lấy toàn bộ danh sách các ID phòng đang mở
    public Set<String> getAllRooms() {
        return chatHistory.keySet();
    }
}
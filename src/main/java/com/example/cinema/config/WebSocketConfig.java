package com.example.cinema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 🔥 ĐÃ FIX: Thay setAllowedOrigins thành setAllowedOriginPatterns("*") 
        // Lớp giáp bảo vệ tuyệt đối giúp Vercel gọi vào WebSocket mà không bị dội ngược 403 CORS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") 
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app"); // Tiền tố khi Client gửi lên Server
        registry.enableSimpleBroker("/topic", "/queue");    // Tiền tố khi Server trả về Client
    }
}
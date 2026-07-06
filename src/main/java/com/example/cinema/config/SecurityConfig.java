package com.example.cinema.config;

import com.example.cinema.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity 
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter authenticationFilter;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults()) 
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 🔥 ĐÃ THÊM: Cho phép Cronjob gọi vào API ping để đánh thức Server mà không cần Token
                .requestMatchers("/api/ping").permitAll()
                
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()

                // QUAN TRỌNG: Cho phép đi qua endpoint lỗi hệ thống để không bị mất CORS headers khi vấp Exception
                .requestMatchers("/error").permitAll()

                // CẤP QUYỀN CHO VNPAY CALLBACK
                .requestMatchers("/api/v1/orders/vnpay-callback").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/v1/movies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/genres/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/cinemas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/cinema-items/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/banners/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/combos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/showtimes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/promotions/**").permitAll()

                .requestMatchers("/api/v1/posts/**").permitAll()
                // Trong file SecurityConfig.java, thêm dòng dưới đây:
                .requestMatchers(HttpMethod.GET, "/api/v1/seats/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                // CẤP QUYỀN CHO VNPAY CALLBACK
                .requestMatchers("/api/v1/orders/vnpay-callback").permitAll()

                // 🔥 ĐÃ THÊM MỚI: Cấp quyền cho đường hầm WebSocket và API lấy lịch sử chat
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/chat/history/**").permitAll()

                
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // FIX: Cho phép cả localhost và 127.0.0.1 để tránh xung đột cổng gọi của trình duyệt
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000","https://akcinema.vercel.app")); 
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
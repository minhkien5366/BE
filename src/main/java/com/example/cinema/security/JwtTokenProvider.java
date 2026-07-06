package com.example.cinema.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    // Lưu ý: Chuỗi Secret Key nên để dài và bảo mật
    private final String jwtSecret = "SecretKeyChoDuAnCinemaCuaBanPhaiDu256BitDeBaoMatTuyetDoi";
    private final long jwtExpirationDate = 86400000; // 24 giờ

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Cập nhật hàm tạo Token để bao gồm các quyền (Roles)
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);

        // Lấy danh sách các quyền của người dùng và nối thành chuỗi cách nhau bởi dấu phẩy
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles) // ĐƯA DANH SÁCH QUYỀN VÀO TOKEN TẠI ĐÂY
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Lấy username từ Token
    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Kiểm tra tính hợp lệ của Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Bạn có thể log lỗi ra đây nếu cần thiết
            return false;
        }
    }
}
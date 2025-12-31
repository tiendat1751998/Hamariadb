package com.datdevops.hamariadb.config;



import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Lớp tiện ích để xử lý các hoạt động liên quan đến JSON Web Token (JWT).
 * Bao gồm tạo token, xác thực token, và trích xuất thông tin từ token.
 */
@Component
public class JwtTokenUtil {

    // Thời gian hết hạn của token (tính bằng giây), đọc từ file cấu hình.
    @Value("${app.security.jwt.expiration}")
    private Long expiration;

    // Khóa bí mật để ký và xác thực token.
    private final SecretKey secretKey;

    /**
     * Constructor, khởi tạo khóa bí mật từ một chuỗi secret trong file cấu hình.
     * @param secret Chuỗi bí mật dùng để tạo khóa.
     */
    public JwtTokenUtil(@Value("${app.security.jwt.secret}") String secret) {
        // Tạo đối tượng SecretKey từ chuỗi secret sử dụng thuật toán HMAC-SHA.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Lấy tên người dùng (username) từ token.
     * Tên người dùng được lưu trong "subject" claim của JWT.
     * @param token Chuỗi JWT.
     * @return Tên người dùng.
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Lấy ngày hết hạn từ token.
     * @param token Chuỗi JWT.
     * @return Ngày hết hạn.
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Một hàm generic để trích xuất một claim cụ thể từ token.
     * @param token Chuỗi JWT.
     * @param claimsResolver Một function để chỉ định cách trích xuất claim.
     * @return Giá trị của claim.
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Trích xuất tất cả các claims (thông tin) từ một token.
     * @param token Chuỗi JWT.
     * @return Đối tượng Claims chứa tất cả thông tin.
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey) // Thiết lập khóa để xác thực chữ ký
                .build()
                .parseClaimsJws(token) // Phân tích và xác thực token
                .getBody();
    }

    /**
     * Kiểm tra xem token đã hết hạn hay chưa.
     * @param token Chuỗi JWT.
     * @return true nếu token đã hết hạn, ngược lại là false.
     */
    private Boolean isTokenExpired(String token) {
        final Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    /**
     * Tạo một token mới cho người dùng.
     * @param userDetails Thông tin chi tiết của người dùng.
     * @return Chuỗi JWT.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Có thể thêm các claims tùy chỉnh vào đây nếu cần
        // ví dụ: claims.put("roles", userDetails.getAuthorities());
        return doGenerateToken(claims, userDetails.getUsername());
    }

    /**
     * Hàm nội bộ để thực hiện việc tạo token.
     * @param claims Các claims tùy chỉnh.
     * @param subject Chủ thể của token (thường là username).
     * @return Chuỗi JWT.
     */
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims) // Đặt các claims tùy chỉnh
                .setSubject(subject) // Đặt chủ thể
                .setIssuedAt(new Date(System.currentTimeMillis())) // Đặt thời gian phát hành
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000)) // Đặt thời gian hết hạn
                .signWith(secretKey, SignatureAlgorithm.HS512) // Ký token bằng khóa bí mật và thuật toán HS512
                .compact(); // Xây dựng và trả về chuỗi token
    }

    /**
     * Xác thực một token.
     * Một token được coi là hợp lệ nếu username trong token khớp và token chưa hết hạn.
     * @param token Chuỗi JWT.
     * @param userDetails Thông tin chi tiết của người dùng để so sánh.
     * @return true nếu token hợp lệ, ngược lại là false.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}

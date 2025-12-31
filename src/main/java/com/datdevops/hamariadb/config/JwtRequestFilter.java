package com.datdevops.hamariadb.config;


import com.datdevops.hamariadb.security.CustomUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Một bộ lọc (filter) của Spring Security chạy một lần cho mỗi request.
 * Nhiệm vụ chính là kiểm tra và xác thực JWT (JSON Web Token) từ header của request.
 * Đây là lớp thay thế cho `JwtAuthenticationFilter` đã bị comment.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    public JwtRequestFilter(CustomUserDetailsService customUserDetailsService, JwtTokenUtil jwtTokenUtil) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * Xử lý logic của bộ lọc cho mỗi request đến.
     * @param request  Đối tượng HttpServletRequest.
     * @param response Đối tượng HttpServletResponse.
     * @param chain    Đối tượng FilterChain để chuyển tiếp request.
     * @throws ServletException
     * @throws IOException
     */
    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Lấy header "Authorization" từ request.
        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // Kiểm tra xem header có tồn tại và có bắt đầu bằng "Bearer " không.
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7); // Cắt bỏ "Bearer " để lấy token.
            try {
                // Lấy username từ token.
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (Exception e) {
                // Ghi log cảnh báo nếu token hết hạn hoặc không hợp lệ.
                logger.warn("JWT Token has expired or is invalid");
            }
        }

        // Khi đã có username và chưa có thông tin xác thực nào trong SecurityContext.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Tải thông tin chi tiết người dùng từ username.
            UserDetails userDetails = this.customUserDetailsService.loadUserByUsername(username);

            // Nếu token hợp lệ, cấu hình Spring Security để xác thực thủ công.
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                // Tạo đối tượng xác thực.
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Đặt thông tin xác thực vào SecurityContext.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // Chuyển tiếp request đến bộ lọc tiếp theo trong chuỗi.
        chain.doFilter(request, response);
    }
}

package com.datdevops.hamariadb.security;


import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.datdevops.hamariadb.config.JwtTokenUtil;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Một bộ lọc (filter) của Spring Security chạy một lần cho mỗi request.
 * Nhiệm vụ chính là kiểm tra và xác thực JWT (JSON Web Token) từ header của request.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
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

        // Lấy header "Authorization" từ request
        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // Kiểm tra xem header có tồn tại và có bắt đầu bằng "Bearer " không
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7); // Cắt bỏ "Bearer " để lấy token
            try {
                // Lấy username từ token
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.error("Không thể lấy JWT Token", e);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token đã hết hạn");
            } catch (Exception e) {
                logger.error("Đã xảy ra lỗi trong quá trình phân tích JWT token", e);
            }
        } else {
            if (requestTokenHeader != null) {
                logger.warn("JWT Token không bắt đầu bằng chuỗi 'Bearer '");
            }
        }


        // Khi đã có token, tiến hành xác thực
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Lấy thông tin chi tiết người dùng từ username
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Nếu token hợp lệ, cấu hình Spring Security để xác thực thủ công
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                // Tạo đối tượng xác thực
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Sau khi thiết lập Authentication trong context, ta chỉ định rằng
                // người dùng hiện tại đã được xác thực. Vì vậy, nó sẽ vượt qua
                // các cấu hình bảo mật của Spring Security thành công.
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        // Chuyển tiếp request đến bộ lọc tiếp theo trong chuỗi
        chain.doFilter(request, response);
    }
}

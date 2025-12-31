package com.datdevops.hamariadb.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.datdevops.hamariadb.security.JwtAuthenticationFilter;

/**
 * Lớp cấu hình chính cho Spring Security.
 * Định nghĩa các quy tắc bảo mật, bộ lọc, và các bean cần thiết.
 */
@Configuration
@EnableWebSecurity // Bật tính năng bảo mật web của Spring Security
@EnableMethodSecurity(prePostEnabled = true) // Bật bảo mật ở cấp độ phương thức (ví dụ: @PreAuthorize)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Tạo bean {@link PasswordEncoder} để mã hóa mật khẩu.
     * Sử dụng BCrypt, một thuật toán mã hóa mạnh và phổ biến.
     * @return một instance của BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tạo bean {@link AuthenticationManager}, cần thiết cho quá trình xác thực.
     * @param authenticationConfiguration Cấu hình xác thực của Spring.
     * @return một instance của AuthenticationManager.
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Cấu hình chuỗi bộ lọc bảo mật (Security Filter Chain).
     * Đây là nơi định nghĩa các quy tắc truy cập cho các endpoint HTTP.
     * @param http Đối tượng HttpSecurity để xây dựng cấu hình.
     * @return một instance của SecurityFilterChain.
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Vô hiệu hóa CSRF (Cross-Site Request Forgery) vì chúng ta sử dụng JWT (stateless).
                .csrf(csrf -> csrf.disable())
                // Cấu hình quy tắc cho các request HTTP.
                .authorizeHttpRequests(auth -> auth
                        // Cho phép tất cả các request đến các endpoint bắt đầu bằng "/v1/auth/" và "/v1/login/".
                        .requestMatchers("/v1/auth/**", "/v1/login/**").permitAll()
                        // Yêu cầu vai trò "ADMIN" cho các request đến "/v1/admin/**".
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        // Tất cả các request còn lại đều yêu cầu phải được xác thực.
                        .anyRequest().authenticated()
                )
                // Cấu hình xử lý ngoại lệ xác thực.
                .exceptionHandling(ex -> ex
                        // Sử dụng JwtAuthenticationEntryPoint để xử lý các lỗi chưa được xác thực (401 Unauthorized).
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                // Cấu hình quản lý session.
                .sessionManagement(session -> session
                        // Thiết lập chính sách là STATELESS, không tạo hoặc sử dụng session phía server.
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // Thêm bộ lọc JwtAuthenticationFilter vào trước bộ lọc UsernamePasswordAuthenticationFilter.
        // Để kiểm tra và xác thực JWT cho mỗi request.
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

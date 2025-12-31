package com.datdevops.hamariadb.config;



import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Lớp này xử lý các trường hợp khi một người dùng chưa được xác thực (unauthenticated)
 * cố gắng truy cập vào một tài nguyên yêu cầu xác thực.
 * Nó implement {@link AuthenticationEntryPoint} của Spring Security.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Phương thức này sẽ được gọi bất cứ khi nào một {@link AuthenticationException} được ném ra.
     * Nó sẽ từ chối request và gửi về một lỗi HTTP 401 (Unauthorized).
     *
     * @param request       request đã gây ra lỗi xác thực.
     * @param response      response để có thể gửi lỗi về cho client.
     * @param authException ngoại lệ xác thực đã được ném ra.
     * @throws IOException
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Gửi mã lỗi 401 Unauthorized cùng với thông điệp "Unauthorized".
        // Client sẽ nhận được lỗi này và biết rằng request của họ cần thông tin xác thực hợp lệ.
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}

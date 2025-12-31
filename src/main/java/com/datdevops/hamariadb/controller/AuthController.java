package com.datdevops.hamariadb.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datdevops.hamariadb.dto.request.ChangePasswordRequest;
import com.datdevops.hamariadb.dto.request.LoginRequest;
import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.dto.response.LoginResponse;
import com.datdevops.hamariadb.service.auth.AuthService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller xử lý các yêu cầu liên quan đến xác thực người dùng như đăng nhập,
 * đổi mật khẩu và đăng xuất.
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint kiểm tra "sức khỏe" của dịch vụ.
     * @return ResponseEntity với mã trạng thái 200 OK.
     */
    @PostMapping("/health")
    public ResponseEntity<Void> health()  {
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint để xử lý yêu cầu đăng nhập của người dùng.
     * @param request Dữ liệu đăng nhập từ client.
     * @return ResponseEntity chứa token và thông tin phiên làm việc nếu đăng nhập thành công.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    /**
     * Endpoint để người dùng đã đăng nhập thay đổi mật khẩu.
     * @param request Dữ liệu yêu cầu thay đổi mật khẩu (mật khẩu cũ, mật khẩu mới).
     * @param authentication Thông tin xác thực của người dùng hiện tại.
     * @return ResponseEntity xác nhận mật khẩu đã được thay đổi thành công.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        authService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    /**
     * Endpoint để xử lý yêu cầu đăng xuất.
     * @param authentication Thông tin xác thực của người dùng hiện tại.
     * @return ResponseEntity xác nhận đăng xuất thành công.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        String username = authentication.getName();
        log.info("User {} logged out", username);
        // Trong một hệ thống thực tế sử dụng JWT, việc đăng xuất ở phía server
        // thường liên quan đến việc đưa token vào danh sách đen (blacklist)
        // để token đó không thể được tái sử dụng cho đến khi hết hạn.
        // Ở đây, chúng ta chỉ đơn giản trả về thông báo thành công.
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }
}

package com.datdevops.hamariadb.service.auth;


import com.datdevops.hamariadb.config.JwtTokenUtil;
import com.datdevops.hamariadb.dto.request.LoginRequest;
import com.datdevops.hamariadb.dto.response.LoginResponse;
import com.datdevops.hamariadb.entity.User;
import com.datdevops.hamariadb.entity.UserStatus;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.security.AESUtil;
import com.datdevops.hamariadb.security.RSAUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.time.LocalDateTime;

/**
 * Service chịu trách nhiệm cho các hoạt động xác thực người dùng,
 * bao gồm đăng nhập và đổi mật khẩu.
 */
@Slf4j
@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AESUtil aesUtil;
    private final RSAUtil rsaUtil;
    private final EncryptionKeyService encryptionKeyService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil,
                       UserDetailsService userDetailsService, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, AESUtil aesUtil, RSAUtil rsaUtil,
                       EncryptionKeyService encryptionKeyService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.aesUtil = aesUtil;
        this.rsaUtil = rsaUtil;
        this.encryptionKeyService = encryptionKeyService;
    }

    /**
     * Xử lý yêu cầu đăng nhập của người dùng.
     * @param request Dữ liệu đăng nhập (username, password, client public key).
     * @return Phản hồi đăng nhập chứa access token và khóa mã hóa session.
     */
    public LoginResponse login(LoginRequest request) {
        try {
            // 1. Xác thực username và password bằng AuthenticationManager của Spring Security.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Nếu xác thực thất bại, cập nhật số lần đăng nhập sai.
            userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                // Khóa tài khoản nếu đăng nhập sai quá 5 lần.
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setStatus(UserStatus.LOCKED);
                }
                userRepository.save(user);
            });
            throw new BadCredentialsException("Invalid username or password");
        }

        // 2. Nếu xác thực thành công, tải thông tin chi tiết người dùng.
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // 3. Tạo JWT (Access Token) cho người dùng.
        final String token = jwtTokenUtil.generateToken(userDetails);

        // 4. Tạo một khóa AES mới cho phiên làm việc này.
        SecretKey aesKey = generateAESKey();

        // 5. Mã hóa khóa AES bằng khóa công khai của client.
        String encryptedAesKey = encryptAESKeyWithRSA(aesKey, request.getClientPublicKey());

        // 6. Lưu trữ khóa mã hóa (cả AES và IV) vào CSDL.
        String keyIdentifier = encryptionKeyService.storeEncryptionKey(
                userDetails.getUsername(), aesKey, request.getClientPublicKey());

        // 7. Cập nhật thông tin đăng nhập của người dùng (lần đăng nhập cuối, reset số lần sai).
        userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        });

        log.info("User {} logged in successfully", request.getUsername());

        // 8. Trả về response chứa token và khóa đã mã hóa.
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenUtil.getExpirationDateFromToken(token).toInstant())
                .encryptedAesKey(encryptedAesKey)
                .keyIdentifier(keyIdentifier)
                .build();
    }

    /**
     * Thay đổi mật khẩu cho người dùng.
     * @param username Tên người dùng.
     * @param currentPassword Mật khẩu hiện tại.
     * @param newPassword Mật khẩu mới.
     */
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Kiểm tra mật khẩu hiện tại có đúng không.
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Mã hóa và cập nhật mật khẩu mới.
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", username);
    }

    /**
     * Tạo một khóa AES mới.
     * @return Đối tượng SecretKey.
     */
    private SecretKey generateAESKey() {
        try {
            return aesUtil.generateKey();
        } catch (Exception e) {
            log.error("Error generating AES key: {}", e.getMessage());
            throw new RuntimeException("Error generating encryption key");
        }
    }

    /**
     * Mã hóa khóa AES bằng khóa công khai RSA của client.
     * @param aesKey Khóa AES cần mã hóa.
     * @param clientPublicKey Khóa công khai của client (dạng chuỗi Base64).
     * @return Chuỗi khóa AES đã được mã hóa và encode Base64.
     */
    private String encryptAESKeyWithRSA(SecretKey aesKey, String clientPublicKey) {
        try {
            PublicKey publicKey = rsaUtil.getPublicKey(clientPublicKey);
            String aesKeyString = aesUtil.convertToString(aesKey);
            return rsaUtil.encrypt(aesKeyString, publicKey);
        } catch (Exception e) {
            log.error("Error encrypting AES key: {}", e.getMessage());
            throw new RuntimeException("Error encrypting session key");
        }
    }
}

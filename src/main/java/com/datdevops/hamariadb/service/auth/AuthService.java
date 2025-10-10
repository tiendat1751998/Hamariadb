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
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Base64;

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

    public LoginResponse login(LoginRequest request) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Update failed login attempts
            userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setStatus(UserStatus.LOCKED);
                }
                userRepository.save(user);
            });
            throw new BadCredentialsException("Invalid username or password");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);

        // Generate AES key for this session
        SecretKey aesKey = generateAESKey();
        String encryptedAesKey = encryptAESKeyWithRSA(aesKey, request.getClientPublicKey());

        // Store encryption key
        String keyIdentifier = encryptionKeyService.storeEncryptionKey(
                userDetails.getUsername(), aesKey, request.getClientPublicKey());

        // Update user login info
        userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        });

        log.info("User {} logged in successfully", request.getUsername());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenUtil.getExpirationDateFromToken(token).toInstant())
                .encryptedAesKey(encryptedAesKey)
                .keyIdentifier(keyIdentifier)
                .build();
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", username);
    }

    private SecretKey generateAESKey() {
        try {
            return aesUtil.generateKey();
        } catch (Exception e) {
            log.error("Error generating AES key: {}", e.getMessage());
            throw new RuntimeException("Error generating encryption key");
        }
    }

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

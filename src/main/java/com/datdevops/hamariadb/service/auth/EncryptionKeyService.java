package com.datdevops.hamariadb.service.auth;


import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.datdevops.hamariadb.entity.EncryptionKey;
import com.datdevops.hamariadb.entity.User;
import com.datdevops.hamariadb.repository.dao.EncryptionKeyRepository;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.security.AESUtil;
import com.datdevops.hamariadb.security.RSAUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý vòng đời của các khóa mã hóa (AES và IV) được sử dụng
 * để bảo mật dữ liệu nhạy cảm trong quá trình giao tiếp.
 */
@Slf4j
@Service
public class EncryptionKeyService {

    private final EncryptionKeyRepository encryptionKeyRepository;
    private final UserRepository userRepository;
    private final AESUtil aesUtil;
    private final RSAUtil rsaUtil;

    // Thời gian hết hạn của khóa (tính bằng giờ), đọc từ file cấu hình.
    @Value("${app.security.encryption.key-expiration-hours}")
    private int keyExpirationHours;

    public EncryptionKeyService(EncryptionKeyRepository encryptionKeyRepository,
                                UserRepository userRepository,
                                AESUtil aesUtil, RSAUtil rsaUtil) {
        this.encryptionKeyRepository = encryptionKeyRepository;
         this.userRepository = userRepository;
        this.aesUtil = aesUtil;
        this.rsaUtil = rsaUtil;
    }

    /**
     * Lưu trữ một cặp khóa AES và IV mới cho người dùng.
     * Khóa AES và IV sẽ được mã hóa bằng khóa công khai của client trước khi lưu vào CSDL.
     *
     * @param username Tên người dùng sở hữu khóa.
     * @param aesKey Khóa AES (SecretKey) cần lưu.
     * @param clientPublicKey Khóa công khai của client (dạng chuỗi Base64) để mã hóa khóa AES.
     * @return Một mã định danh duy nhất (keyIdentifier) cho cặp khóa đã lưu.
     */
    public String storeEncryptionKey(String username, SecretKey aesKey, String clientPublicKey) {
        try {
            // Vô hiệu hóa các khóa cũ của người dùng này để đảm bảo chỉ có một khóa active tại một thời điểm.
            encryptionKeyRepository.deactivateUserKeys(username);

            // Lấy đối tượng PublicKey từ chuỗi Base64.
            PublicKey publicKey = rsaUtil.getPublicKey(clientPublicKey);
            // Chuyển khóa AES thành chuỗi Base64.
            String aesKeyString = aesUtil.convertToString(aesKey);
            // Mã hóa chuỗi khóa AES bằng khóa công khai của client.
            String encryptedAesKey = rsaUtil.encrypt(aesKeyString, publicKey);

            // Tạo một IV mới và mã hóa nó bằng khóa công khai của client.
            byte[] iv = aesUtil.generateIV();
            String encryptedIv = rsaUtil.encrypt(Base64.getEncoder().encodeToString(iv), publicKey);

            // Tìm đối tượng User từ username.
            User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalStateException("User not found with username: " + username));

            // Tạo và lưu bản ghi EncryptionKey vào CSDL.
            EncryptionKey encryptionKey = new EncryptionKey();
            encryptionKey.setKeyIdentifier(generateKeyIdentifier());
            encryptionKey.setUser(user);
            encryptionKey.setAesKeyEncrypted(encryptedAesKey);
            encryptionKey.setIvEncrypted(encryptedIv);
            encryptionKey.setClientPublicKey(clientPublicKey);
            encryptionKey.setExpiresAt(LocalDateTime.now().plusHours(keyExpirationHours));
            encryptionKey.setIsActive(true);

            encryptionKeyRepository.save(encryptionKey);

            return encryptionKey.getKeyIdentifier();

        } catch (Exception e) {
            log.error("Error storing encryption key: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store encryption key", e);
        }
    }

    /**
     * Lấy lại khóa AES (SecretKey) từ CSDL.
     * @param keyIdentifier Mã định danh của khóa.
     * @param clientPrivateKey Khóa riêng tư của client (dạng chuỗi) để giải mã khóa AES.
     * @return Đối tượng SecretKey.
     */
    public SecretKey getAESKey(String keyIdentifier, String clientPrivateKey) {
        try {
            // Tìm khóa đang active trong CSDL.
            EncryptionKey encryptionKey = encryptionKeyRepository.findByKeyIdentifierAndIsActive(keyIdentifier, true)
                    .orElseThrow(() -> new RuntimeException("Encryption key not found or expired"));

            // Giải mã chuỗi khóa AES bằng khóa riêng tư của client.
            String decryptedAesKey = rsaUtil.decrypt(encryptionKey.getAesKeyEncrypted(), clientPrivateKey);
            // Chuyển chuỗi đã giải mã thành đối tượng SecretKey.
            return aesUtil.convertToAESKey(decryptedAesKey);

        } catch (Exception e) {
            log.error("Error retrieving AES key: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve encryption key", e);
        }
    }

    /**
     * Lấy lại IV (Initialization Vector) từ CSDL.
     * @param keyIdentifier Mã định danh của khóa.
     * @param clientPrivateKey Khóa riêng tư của client (dạng chuỗi) để giải mã IV.
     * @return Mảng byte chứa IV.
     */
    public byte[] getIV(String keyIdentifier, String clientPrivateKey) {
        try {
            EncryptionKey encryptionKey = encryptionKeyRepository.findByKeyIdentifierAndIsActive(keyIdentifier, true)
                    .orElseThrow(() -> new RuntimeException("Encryption key not found or expired"));

            // Giải mã chuỗi IV bằng khóa riêng tư của client.
            String decryptedIv = rsaUtil.decrypt(encryptionKey.getIvEncrypted(), clientPrivateKey);
            // Decode chuỗi Base64 thành mảng byte.
            return Base64.getDecoder().decode(decryptedIv);

        } catch (Exception e) {
            log.error("Error retrieving IV: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve IV", e);
        }
    }

    /**
     * Tạo một mã định danh khóa duy nhất.
     * @return Chuỗi mã định danh.
     */
    private String generateKeyIdentifier() {
        return "key_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Vô hiệu hóa tất cả các khóa đã hết hạn.
     * Thường được gọi bởi một scheduler.
     */
    public void cleanupExpiredKeys() {
        encryptionKeyRepository.deactivateExpiredKeys();
        log.info("Expired encryption keys cleaned up");
    }
}

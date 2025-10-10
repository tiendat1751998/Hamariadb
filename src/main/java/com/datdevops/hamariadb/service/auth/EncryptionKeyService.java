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

@Slf4j
@Service
public class EncryptionKeyService {

    private final EncryptionKeyRepository encryptionKeyRepository;
    private final UserRepository userRepository;
    private final AESUtil aesUtil;
    private final RSAUtil rsaUtil;

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

    public String storeEncryptionKey(String username, SecretKey aesKey, String clientPublicKey) {
        try {
            // Deactivate previous keys for this user
            encryptionKeyRepository.deactivateUserKeys(username);

            // Encrypt AES key with client's public key
            PublicKey publicKey = rsaUtil.getPublicKey(clientPublicKey);
            String aesKeyString = aesUtil.convertToString(aesKey);
            String encryptedAesKey = rsaUtil.encrypt(aesKeyString, publicKey);

            // Generate IV and encrypt it
            byte[] iv = aesUtil.generateIV();
            String encryptedIv = rsaUtil.encrypt(Base64.getEncoder().encodeToString(iv), publicKey);
            User  user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalStateException(" user not found username"+ username));
            // Create and save encryption key record
            EncryptionKey encryptionKey = new EncryptionKey();
            encryptionKey.setKeyIdentifier(generateKeyIdentifier());
            encryptionKey.setUser(user); // Set user from repository
            encryptionKey.setAesKeyEncrypted(encryptedAesKey);
            encryptionKey.setIvEncrypted(encryptedIv);
            encryptionKey.setClientPublicKey(clientPublicKey);
            encryptionKey.setExpiresAt(LocalDateTime.now().plusHours(keyExpirationHours));
            encryptionKey.setIsActive(true);

            encryptionKeyRepository.save(encryptionKey);

            return encryptionKey.getKeyIdentifier();

        } catch (Exception e) {
            log.error("Error storing encryption key: {}", e.getMessage(),e);
            throw new RuntimeException("Failed to store encryption key",e);
        }
    }

    public SecretKey getAESKey(String keyIdentifier, String clientPrivateKey) {
        try {
            EncryptionKey encryptionKey = encryptionKeyRepository.findByKeyIdentifierAndIsActive(keyIdentifier, true)
                    .orElseThrow(() -> new RuntimeException("Encryption key not found or expired"));

            // Decrypt AES key with client's private key
            String decryptedAesKey = rsaUtil.decrypt(encryptionKey.getAesKeyEncrypted(), clientPrivateKey);
            return aesUtil.convertToAESKey(decryptedAesKey);

        } catch (Exception e) {
            log.error("Error retrieving AES key: {}", e.getMessage(),e);
            throw new RuntimeException("Failed to retrieve encryption key",e);
        }
    }

    public byte[] getIV(String keyIdentifier, String clientPrivateKey) {
        try {
            EncryptionKey encryptionKey = encryptionKeyRepository.findByKeyIdentifierAndIsActive(keyIdentifier, true)
                    .orElseThrow(() -> new RuntimeException("Encryption key not found or expired"));

            // Decrypt IV with client's private key
            String decryptedIv = rsaUtil.decrypt(encryptionKey.getIvEncrypted(), clientPrivateKey);
            return Base64.getDecoder().decode(decryptedIv);

        } catch (Exception e) {
            log.error("Error retrieving IV: {}", e.getMessage(),e);
            throw new RuntimeException("Failed to retrieve IV",e);
        }
    }

    private String generateKeyIdentifier() {
        return "key_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void cleanupExpiredKeys() {
        encryptionKeyRepository.deactivateExpiredKeys();
        log.info("Expired encryption keys cleaned up");
    }
   
}

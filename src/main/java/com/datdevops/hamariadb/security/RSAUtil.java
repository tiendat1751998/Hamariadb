package com.datdevops.hamariadb.security;


import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Lớp tiện ích để thực hiện các hoạt động mã hóa bất đối xứng sử dụng thuật toán RSA.
 * Bao gồm mã hóa, giải mã, tạo cặp khóa, ký và xác thực chữ ký số.
 */
@Slf4j
@Component
public class RSAUtil {

    // Kích thước khóa RSA (ví dụ: 2048, 4096), đọc từ file cấu hình.
    @Value("${app.security.encryption.rsa-key-size}")
    private int keySize;
    // Khóa riêng tư của server (dưới dạng chuỗi Base64), đọc từ file cấu hình.
    @Value("${app.security.server.private-key}")
    private String serverPrivateKeyString;

    /**
     * Tạo một cặp khóa RSA mới (gồm khóa công khai và khóa riêng tư).
     * @return Đối tượng KeyPair chứa cặp khóa.
     * @throws NoSuchAlgorithmException nếu thuật toán RSA không được hỗ trợ.
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Mã hóa dữ liệu bằng khóa công khai (Public Key).
     * @param data Dữ liệu cần mã hóa.
     * @param publicKey Khóa công khai để mã hóa.
     * @return Chuỗi đã được mã hóa và encode Base64.
     * @throws Exception nếu có lỗi trong quá trình mã hóa.
     */
    public String encrypt(String data, PublicKey publicKey) throws Exception {
        // Sử dụng padding OAEP để tăng cường bảo mật so với PKCS1Padding
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Giải mã dữ liệu bằng khóa riêng tư (Private Key).
     * @param encryptedData Dữ liệu đã mã hóa (dạng Base64).
     * @param privateKey Chuỗi khóa riêng tư (định dạng PEM hoặc chỉ Base64).
     * @return Dữ liệu gốc đã được giải mã.
     * @throws Exception nếu có lỗi trong quá trình giải mã (sai khóa, dữ liệu hỏng...).
     */
    public String decrypt(String encryptedData, String privateKey) throws Exception {
        // Xóa các header/footer và ký tự xuống dòng của định dạng PEM.
        privateKey = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        // Giải mã Base64 để lấy mảng byte của khóa.
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);

        // Tạo đối tượng PrivateKey từ mảng byte theo định dạng PKCS8.
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey key = keyFactory.generatePrivate(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedData = cipher.doFinal(decodedData);
        return new String(decryptedData);
    }

    /**
     * Chuyển đổi một chuỗi khóa công khai (Base64) thành đối tượng PublicKey.
     * @param base64PublicKey Chuỗi khóa công khai dạng Base64.
     * @return Đối tượng PublicKey.
     * @throws Exception nếu định dạng khóa không hợp lệ.
     */
    public PublicKey getPublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Chuyển đổi một chuỗi khóa riêng tư (Base64) thành đối tượng PrivateKey.
     * @param base64PrivateKey Chuỗi khóa riêng tư dạng Base64.
     * @return Đối tượng PrivateKey.
     * @throws Exception nếu định dạng khóa không hợp lệ.
     */
    public PrivateKey getPrivateKey(String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Tạo chữ ký số cho dữ liệu bằng khóa riêng tư của server.
     * @param data Dữ liệu cần ký.
     * @return Chữ ký số dưới dạng chuỗi Base64.
     * @throws Exception nếu có lỗi trong quá trình ký.
     */
     public String sign(String data) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(getServerPrivateKey());
        privateSignature.update(data.getBytes());
        byte[] signature = privateSignature.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * Chuyển đổi một đối tượng Key (PublicKey hoặc PrivateKey) thành chuỗi Base64.
     * @param key Đối tượng khóa.
     * @return Chuỗi Base64 đại diện cho khóa.
     */
    public String convertToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Lấy khóa riêng tư của server từ chuỗi cấu hình.
     * @return Đối tượng PrivateKey của server.
     * @throws Exception nếu có lỗi khi phân tích khóa.
     */
       public PrivateKey getServerPrivateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(serverPrivateKeyString);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}

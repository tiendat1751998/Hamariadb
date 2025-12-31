package com.datdevops.hamariadb.security;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Lớp tiện ích để thực hiện mã hóa và giải mã đối xứng bằng thuật toán AES.
 * Sử dụng chế độ GCM (Galois/Counter Mode) để đảm bảo cả tính bí mật và tính toàn vẹn của dữ liệu.
 */
@Slf4j
@Component
public class AESUtil {

    // Thuật toán và chế độ mã hóa được sử dụng. AES/GCM/NoPadding là một lựa chọn hiện đại và an toàn.
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    // Độ dài của Authentication Tag (tính bằng bit). 128 bit là giá trị phổ biến và an toàn.
    private static final int TAG_LENGTH_BIT = 128;
    // Độ dài của Initialization Vector (IV) (tính bằng byte). 12 byte là độ dài được khuyến nghị cho GCM.
    private static final int IV_LENGTH_BYTE = 12;

    // Kích thước khóa AES (ví dụ: 128, 192, 256), được đọc từ file cấu hình.
    @Value("${app.security.encryption.aes-key-size}")
    private int keySize;

    /**
     * Tạo một khóa bí mật (SecretKey) AES mới.
     * @return Một đối tượng SecretKey.
     * @throws NoSuchAlgorithmException nếu thuật toán AES không được hỗ trợ.
     */
    public SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySize);
        return keyGenerator.generateKey();
    }

    /**
     * Tạo một Initialization Vector (IV) ngẫu nhiên.
     * IV là cần thiết cho chế độ GCM và phải là duy nhất cho mỗi lần mã hóa với cùng một khóa.
     * @return Một mảng byte chứa IV.
     */
    public byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH_BYTE];
        new SecureRandom().nextBytes(iv); // Sử dụng SecureRandom để đảm bảo tính ngẫu nhiên cao.
        return iv;
    }

    /**
     * Mã hóa một chuỗi dữ liệu sử dụng khóa và IV cho trước.
     * @param data Dữ liệu cần mã hóa.
     * @param key Khóa bí mật AES.
     * @param iv Initialization Vector.
     * @return Chuỗi đã được mã hóa và encode Base64.
     * @throws Exception nếu có lỗi trong quá trình mã hóa.
     */
    public String encrypt(String data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        // Encode kết quả thành Base64 để dễ dàng truyền đi hoặc lưu trữ.
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Giải mã một chuỗi dữ liệu đã được mã hóa.
     * @param encryptedData Chuỗi đã mã hóa (dạng Base64).
     * @param key Khóa bí mật AES đã dùng để mã hóa.
     * @param iv Initialization Vector đã dùng để mã hóa.
     * @return Chuỗi dữ liệu gốc đã được giải mã.
     * @throws Exception nếu có lỗi trong quá trình giải mã (ví dụ: sai khóa, dữ liệu bị thay đổi).
     */
    public String decrypt(String encryptedData, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        // Decode chuỗi Base64 trở lại thành mảng byte.
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedData = cipher.doFinal(decodedData);
        return new String(decryptedData);
    }

    /**
     * Chuyển đổi một chuỗi khóa đã được encode Base64 thành đối tượng SecretKey.
     * @param encodedKey Chuỗi khóa dạng Base64.
     * @return Đối tượng SecretKey.
     */
    public SecretKey convertToAESKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    /**
     * Chuyển đổi một đối tượng SecretKey thành chuỗi Base64.
     * @param secretKey Đối tượng SecretKey cần chuyển đổi.
     * @return Chuỗi Base64 đại diện cho khóa.
     */
    public String convertToString(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}

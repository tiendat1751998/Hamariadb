package com.datdevops.hamariadb.scheduler;


import com.datdevops.hamariadb.service.auth.EncryptionKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lớp Scheduler để tự động dọn dẹp các khóa mã hóa đã hết hạn.
 */
@Slf4j
@Component
public class EncryptionKeyCleanupScheduler {

    private final EncryptionKeyService encryptionKeyService;

    /**
     * Constructor để inject EncryptionKeyService.
     */
    public EncryptionKeyCleanupScheduler(EncryptionKeyService encryptionKeyService) {
        this.encryptionKeyService = encryptionKeyService;
    }

    /**
     * Phương thức được lên lịch để chạy tự động.
     * Cron "0 0 2 * * ?" có nghĩa là "chạy vào lúc 2:00:00 sáng mỗi ngày".
     * Mục đích là để vô hiệu hóa các khóa mã hóa đã quá thời gian sử dụng.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Chạy vào 2:00 sáng hàng ngày
    public void cleanupExpiredEncryptionKeys() {
        log.info("Bắt đầu quá trình dọn dẹp khóa mã hóa hết hạn...");

        try {
            // Gọi service để thực hiện việc dọn dẹp
            encryptionKeyService.cleanupExpiredKeys();
            log.info("Dọn dẹp khóa mã hóa hết hạn hoàn tất thành công.");
        } catch (Exception e) {
            // Ghi lại lỗi nếu có bất kỳ ngoại lệ nào xảy ra
            log.error("Lỗi trong quá trình dọn dẹp khóa mã hóa: {}", e.getMessage(), e);
        }
    }
}

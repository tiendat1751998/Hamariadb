package com.datdevops.hamariadb.scheduler;


import com.datdevops.hamariadb.service.auth.EncryptionKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EncryptionKeyCleanupScheduler {

    private final EncryptionKeyService encryptionKeyService;

    public EncryptionKeyCleanupScheduler(EncryptionKeyService encryptionKeyService) {
        this.encryptionKeyService = encryptionKeyService;
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2:00 AM
    public void cleanupExpiredEncryptionKeys() {
        log.info("Starting encryption key cleanup...");

        try {
            encryptionKeyService.cleanupExpiredKeys();
            log.info("Encryption key cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error cleaning up encryption keys: {}", e.getMessage(), e);
        }
    }
}

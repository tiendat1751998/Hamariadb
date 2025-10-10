package com.datdevops.hamariadb.scheduler;


import com.datdevops.hamariadb.service.tranfer.ScheduledTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScheduledTransferScheduler {

    private final ScheduledTransferService scheduledTransferService;

    public ScheduledTransferScheduler(ScheduledTransferService scheduledTransferService) {
        this.scheduledTransferService = scheduledTransferService;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void processDueScheduledTransfers() {
        log.info("Starting scheduled transfer processing...");

        try {
            scheduledTransferService.processDueScheduledTransfers();
            log.info("Scheduled transfer processing completed successfully");
        } catch (Exception e) {
            log.error("Error processing scheduled transfers: {}", e.getMessage(), e);
        }
    }
}

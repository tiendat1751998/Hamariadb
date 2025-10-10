package com.datdevops.hamariadb.scheduler;


import com.datdevops.hamariadb.service.tranfer.RecurringTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RecurringTransferScheduler {

    private final RecurringTransferService recurringTransferService;

    public RecurringTransferScheduler(RecurringTransferService recurringTransferService) {
        this.recurringTransferService = recurringTransferService;
    }

    @Scheduled(cron = "0 0 6 * * ?") // Run daily at 6:00 AM
    public void processDueRecurringTransfers() {
        log.info("Starting recurring transfer processing...");

        try {
            recurringTransferService.processDueRecurringTransfers();
            log.info("Recurring transfer processing completed successfully");
        } catch (Exception e) {
            log.error("Error processing recurring transfers: {}", e.getMessage(), e);
        }
    }
}

package com.datdevops.hamariadb.scheduler;


import com.datdevops.hamariadb.service.tranfer.RecurringTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lớp Scheduler để tự động xử lý các giao dịch chuyển tiền định kỳ đến hạn.
 */
@Slf4j
@Component
public class RecurringTransferScheduler {

    private final RecurringTransferService recurringTransferService;

    /**
     * Constructor để inject RecurringTransferService.
     */
    public RecurringTransferScheduler(RecurringTransferService recurringTransferService) {
        this.recurringTransferService = recurringTransferService;
    }

    /**
     * Phương thức được lên lịch để chạy tự động.
     * Sử dụng biểu thức cron để định nghĩa thời gian chạy.
     * Cron "0 0 6 * * ?" có nghĩa là "chạy vào lúc 6:00:00 sáng mỗi ngày".
     */
    @Scheduled(cron = "0 0 6 * * ?") // Chạy vào 6:00 sáng hàng ngày
    public void processDueRecurringTransfers() {
        log.info("Bắt đầu xử lý các giao dịch định kỳ đến hạn...");

        try {
            // Gọi service để tìm và xử lý các giao dịch đến hạn
            recurringTransferService.processDueRecurringTransfers();
            log.info("Xử lý các giao dịch định kỳ đến hạn hoàn tất thành công.");
        } catch (Exception e) {
            // Ghi lại lỗi nếu có bất kỳ ngoại lệ nào xảy ra trong quá trình xử lý
            log.error("Lỗi trong quá trình xử lý giao dịch định kỳ: {}", e.getMessage(), e);
        }
    }
}

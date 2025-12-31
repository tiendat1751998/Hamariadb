package com.datdevops.hamariadb.service.tranfer;

import com.datdevops.hamariadb.dto.request.BatchTransferItemRequest;
import com.datdevops.hamariadb.dto.request.BatchTransferRequest;
import com.datdevops.hamariadb.dto.response.BatchTransferResponse;
import com.datdevops.hamariadb.entity.*;
import com.datdevops.hamariadb.repository.dao.BatchTransferRepository;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.repository.mapper.EntityMapper;
import com.datdevops.hamariadb.service.notification.TelegramService;
import com.datdevops.hamariadb.util.ExcelFileProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service để xử lý các giao dịch chuyển tiền theo lô.
 */
@Slf4j
@Service
@Transactional
public class BatchTransferService {

    // Repository để tương tác với dữ liệu giao dịch lô
    private final BatchTransferRepository batchTransferRepository;
    // Repository để tương tác với dữ liệu người dùng
    private final UserRepository userRepository;
    // Service để xử lý file Excel
    private final ExcelFileProcessor excelFileProcessor;
    // Service để gửi thông báo qua Telegram
    private final TelegramService telegramService;
    // Service để xử lý các giao dịch chuyển tiền đơn lẻ
    private final TransferService transferService;
    // Mapper để chuyển đổi giữa Entity và DTO
    private final EntityMapper entityMapper;

    // Giới hạn số dòng tối đa trong một file batch
    @Value("${app.transfer.batch-max-rows}")
    private int batchMaxRows;

    /**
     * Constructor để inject các dependency.
     */
    public BatchTransferService(BatchTransferRepository batchTransferRepository,
                                UserRepository userRepository,
                                ExcelFileProcessor excelFileProcessor,
                                TelegramService telegramService,
                                TransferService transferService,
                                EntityMapper entityMapper) {
        this.batchTransferRepository = batchTransferRepository;
        this.userRepository = userRepository;
        this.excelFileProcessor = excelFileProcessor;
        this.telegramService = telegramService;
        this.transferService = transferService;
        this.entityMapper = entityMapper;
    }

    /**
     * Chuyển đổi một đối tượng BatchTransfer (Entity) thành BatchTransferResponse (DTO).
     * @param batchTransfer Đối tượng Entity cần chuyển đổi.
     * @return Đối tượng DTO.
     */
    @SuppressWarnings("unused")
    private BatchTransferResponse convertToBatchTransferResponse(BatchTransfer batchTransfer) {
        return BatchTransferResponse.builder()
                .batchId(batchTransfer.getBatchId())
                .fileName(batchTransfer.getFileName())
                .fileType(batchTransfer.getFileType())
                .totalTransactions(batchTransfer.getTotalTransactions())
                .successfulTransactions(batchTransfer.getSuccessfulTransactions())
                .failedTransactions(batchTransfer.getFailedTransactions())
                .totalAmount(batchTransfer.getTotalAmount())
                .channel(batchTransfer.getChannel())
                .status(batchTransfer.getStatus())
                .initiatedBy(batchTransfer.getInitiatedBy().getUsername())
                .approvedBy(batchTransfer.getApprovedBy() != null ? batchTransfer.getApprovedBy().getUsername() : null)
                .createdAt(batchTransfer.getCreatedAt())
                .processedAt(batchTransfer.getProcessedAt())
                .build();
    }

    /**
     * Lấy thông tin chi tiết của một giao dịch lô.
     * @param batchId ID của lô.
     * @param username Tên người dùng thực hiện.
     * @return Thông tin chi tiết của lô.
     */
    public BatchTransferResponse getBatchTransfer(String batchId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BatchTransfer batchTransfer = batchTransferRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch transfer not found"));

        // Xác thực người dùng có quyền sở hữu giao dịch lô này
        if (!batchTransfer.getInitiatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this batch transfer");
        }
        return entityMapper.toBatchTransferResponse(batchTransfer);
    }

    /**
     * Lấy danh sách các giao dịch lô của một người dùng.
     * @param username Tên người dùng.
     * @return Danh sách các giao dịch lô.
     */
    public List<BatchTransferResponse> getBatchTransfers(String username) {
        // Kiểm tra sự tồn tại của người dùng
        userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<BatchTransfer> batchTransfers = batchTransferRepository.findByUsername(username);

        // Chuyển đổi danh sách Entity thành danh sách DTO
        return batchTransfers.stream()
                .map(entityMapper::toBatchTransferResponse)
                .collect(Collectors.toList());
    }

    /**
     * Xử lý một yêu cầu chuyển tiền theo lô từ file.
     * @param request Thông tin yêu cầu lô.
     * @param file File Excel chứa danh sách giao dịch.
     * @param username Tên người dùng thực hiện.
     * @return Phản hồi chứa thông tin ban đầu của lô.
     */
    public BatchTransferResponse processBatchTransfer(BatchTransferRequest request,
                                                      MultipartFile file,
                                                      String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Xử lý file Excel để lấy danh sách các mục giao dịch
            List<BatchTransferItemRequest> items = excelFileProcessor.processExcelFile(file);

            // Kiểm tra giới hạn số dòng
            if (items.size() > batchMaxRows) {
                throw new RuntimeException("File contains too many rows. Maximum allowed: " + batchMaxRows);
            }

            // Tạo bản ghi cho giao dịch lô
            BatchTransfer batchTransfer = createBatchTransfer(request, file, user, items);


            // Xử lý các mục trong lô (có thể thực hiện bất đồng bộ)
            processBatchItems(batchTransfer, items, user);

            // Trả về phản hồi ban đầu cho người dùng
            return BatchTransferResponse.builder()
                    .batchId(batchTransfer.getBatchId())
                    .status(batchTransfer.getStatus())
                    .totalTransactions(batchTransfer.getTotalTransactions())
                    .message("Batch transfer submitted for processing")
                    .build();

        } catch (Exception e) {
            log.error("Batch transfer processing failed: {}", e.getMessage());
            throw new RuntimeException("Failed to process batch transfer: " + e.getMessage());
        }
    }

    /**
     * Tạo một bản ghi BatchTransfer mới trong cơ sở dữ liệu.
     * @param request Dữ liệu yêu cầu.
     * @param file File được tải lên.
     * @param user Người dùng khởi tạo.
     * @param items Danh sách các mục giao dịch.
     * @return Đối tượng BatchTransfer đã được lưu.
     */
    private BatchTransfer createBatchTransfer(BatchTransferRequest request,
                                              MultipartFile file,
                                              User user,
                                              List<BatchTransferItemRequest> items) {
        BatchTransfer batchTransfer = new BatchTransfer();
        batchTransfer.setBatchId(generateBatchId());
        batchTransfer.setFileName(file.getOriginalFilename());
        batchTransfer.setFileType(getFileExtension(file.getOriginalFilename()));
        batchTransfer.setTotalTransactions(items.size());
        batchTransfer.setChannel(request.getChannel());
        batchTransfer.setStatus(BatchTransferStatus.VALIDATED); // Giả sử đã xác thực
        batchTransfer.setInitiatedBy(user);

        // Tính tổng số tiền từ các mục
        BigDecimal totalAmount = items.stream()
                .map(BatchTransferItemRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        batchTransfer.setTotalAmount(totalAmount);

        return batchTransferRepository.save(batchTransfer);
    }

    /**
     * Xử lý từng mục trong một giao dịch lô.
     * @param batchTransfer Đối tượng lô cha.
     * @param items Danh sách các mục cần xử lý.
     * @param user Người dùng thực hiện.
     */
    private void processBatchItems(BatchTransfer batchTransfer,
                                   List<BatchTransferItemRequest> items,
                                   User user) {
        List<BatchTransferItem> batchItems = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // Lặp qua từng mục để xử lý
        for (BatchTransferItemRequest item : items) {
            BatchTransferItem batchItem = createBatchTransferItem(batchTransfer, item);

            try {
                // Xử lý giao dịch đơn lẻ
                processSingleBatchItem(batchItem, user);
                batchItem.setStatus(TransferStatus.COMPLETED);
                successCount++;
            } catch (Exception e) {
                batchItem.setStatus(TransferStatus.FAILED);
                batchItem.setErrorMessage(e.getMessage());
                failureCount++;
            }

            batchItems.add(batchItem);
        }

        // Cập nhật trạng thái cuối cùng của lô
        batchTransfer.setSuccessfulTransactions(successCount);
        batchTransfer.setFailedTransactions(failureCount);
        batchTransfer.setStatus(determineFinalStatus(successCount, failureCount, items.size()));
        batchTransfer.setProcessedAt(LocalDateTime.now());
        batchTransfer.setBatchTransferItems(batchItems);
        batchTransferRepository.save(batchTransfer);

        // Gửi thông báo kết quả xử lý
        telegramService.sendBatchProcessingResult(
                batchTransfer.getBatchId(),
                items.size(),
                successCount,
                failureCount,
                batchTransfer.getTotalAmount()
        );
    }

    /**
     * Tạo một đối tượng BatchTransferItem từ request.
     * @param batchTransfer Lô cha.
     * @param item Dữ liệu yêu cầu cho mục.
     * @return Đối tượng BatchTransferItem.
     */
    private BatchTransferItem createBatchTransferItem(BatchTransfer batchTransfer,
                                                      BatchTransferItemRequest item) {
        BatchTransferItem batchItem = new BatchTransferItem();
        batchItem.setBatchTransfer(batchTransfer);
        batchItem.setSequenceNumber(item.getSequenceNumber());
        batchItem.setToAccountNumber(item.getToAccountNumber());
        batchItem.setToBankCode(item.getToBankCode());
        batchItem.setToAccountName(item.getToAccountName());
        batchItem.setAmount(item.getAmount());
        batchItem.setDescription(item.getDescription());
        batchItem.setStatus(TransferStatus.PENDING);

        return batchItem;
    }

    /**
     * Xử lý một giao dịch đơn lẻ trong lô.
     * @param item Mục giao dịch cần xử lý.
     * @param user Người dùng thực hiện.
     */
    private void processSingleBatchItem(BatchTransferItem item, User user) {
        // Logic xử lý chuyển tiền cho một mục.
        // Trong thực tế, sẽ gọi đến TransferService.
        log.info("Processing batch item: {} to {}", item.getAmount(), item.getToAccountNumber());

        // Giả lập thời gian xử lý
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Tạo một ID duy nhất cho lô.
     * @return ID của lô.
     */
    private String generateBatchId() {
        return "BATCH_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * Lấy phần mở rộng của file.
     * @param filename Tên file.
     * @return Phần mở rộng (ví dụ: "xlsx").
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastIndex = filename.lastIndexOf('.');
        return lastIndex == -1 ? "" : filename.substring(lastIndex + 1);
    }

    /**
     * Xác định trạng thái cuối cùng của lô dựa trên kết quả các giao dịch con.
     * @param success Số lượng thành công.
     * @param failure Số lượng thất bại.
     * @param total Tổng số giao dịch.
     * @return Trạng thái cuối cùng của lô.
     */
    private BatchTransferStatus determineFinalStatus(int success, int failure, int total) {
        if (failure == 0) return BatchTransferStatus.COMPLETED; // Hoàn thành tất cả
        if (success == 0) return BatchTransferStatus.FAILED;    // Thất bại tất cả
        return BatchTransferStatus.PARTIALLY_COMPLETED;         // Hoàn thành một phần
    }
}

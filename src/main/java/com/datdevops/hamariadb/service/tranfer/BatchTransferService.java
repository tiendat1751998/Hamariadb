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

@Slf4j
@Service
@Transactional
public class BatchTransferService {

    private final BatchTransferRepository batchTransferRepository;
    private final UserRepository userRepository;
    private final ExcelFileProcessor excelFileProcessor;
    private final TelegramService telegramService;
    private final TransferService transferService;
    private  final EntityMapper entityMapper;

    @Value("${app.transfer.batch-max-rows}")
    private int batchMaxRows;

    public BatchTransferService(BatchTransferRepository batchTransferRepository,
                                UserRepository userRepository,
                                ExcelFileProcessor excelFileProcessor,
                                TelegramService telegramService,
                                TransferService transferService, EntityMapper entityMapper) {
        this.batchTransferRepository = batchTransferRepository;
        this.userRepository = userRepository;
        this.excelFileProcessor = excelFileProcessor;
        this.telegramService = telegramService;
        this.transferService = transferService;

        this.entityMapper = entityMapper;
    }
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
    public BatchTransferResponse getBatchTransfer(String batchId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BatchTransfer batchTransfer = batchTransferRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch transfer not found"));

        // Verify user owns the batch transfer
        if (!batchTransfer.getInitiatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this batch transfer");
        }

        return entityMapper.toBatchTransferResponse(batchTransfer);
    }

    public List<BatchTransferResponse> getBatchTransfers(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<BatchTransfer> batchTransfers = batchTransferRepository.findByUsername(username);

        return batchTransfers.stream()
                .map(entityMapper::toBatchTransferResponse)
                .collect(Collectors.toList());
    }

    public BatchTransferResponse processBatchTransfer(BatchTransferRequest request,
                                                      MultipartFile file,
                                                      String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Process Excel file
            List<BatchTransferItemRequest> items = excelFileProcessor.processExcelFile(file);

            if (items.size() > batchMaxRows) {
                throw new RuntimeException("File contains too many rows. Maximum allowed: " + batchMaxRows);
            }

            // Create batch transfer record
            BatchTransfer batchTransfer = createBatchTransfer(request, file, user, items);


            // Process items asynchronously (in real implementation, this would be queued)
            processBatchItems(batchTransfer, items, user);

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
        batchTransfer.setStatus(BatchTransferStatus.VALIDATED);
        batchTransfer.setInitiatedBy(user);

        // Calculate total amount
        BigDecimal totalAmount = items.stream()
                .map(BatchTransferItemRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        batchTransfer.setTotalAmount(totalAmount);

        return batchTransferRepository.save(batchTransfer);
    }

    private void processBatchItems(BatchTransfer batchTransfer,
                                   List<BatchTransferItemRequest> items,
                                   User user) {
        List<BatchTransferItem> batchItems = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (BatchTransferItemRequest item : items) {
            BatchTransferItem batchItem = createBatchTransferItem(batchTransfer, item);

            try {
                // Process individual transfer
                // This would typically involve calling the transfer service
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

        // Update batch transfer status
        batchTransfer.setSuccessfulTransactions(successCount);
        batchTransfer.setFailedTransactions(failureCount);
        batchTransfer.setStatus(determineFinalStatus(successCount, failureCount, items.size()));
        batchTransfer.setProcessedAt(LocalDateTime.now());
        batchTransfer.setBatchTransferItems(batchItems);
        batchTransferRepository.save(batchTransfer);

        // Send notification
        telegramService.sendBatchProcessingResult(
                batchTransfer.getBatchId(),
                items.size(),
                successCount,
                failureCount,
                batchTransfer.getTotalAmount()
        );
    }

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

    private void processSingleBatchItem(BatchTransferItem item, User user) {
        // Implement individual transfer processing
        // This would call the main transfer service with appropriate parameters
        log.info("Processing batch item: {} to {}", item.getAmount(), item.getToAccountNumber());

        // Simulate processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateBatchId() {
        return "BATCH_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastIndex = filename.lastIndexOf('.');
        return lastIndex == -1 ? "" : filename.substring(lastIndex + 1);
    }

    private BatchTransferStatus determineFinalStatus(int success, int failure, int total) {
        if (failure == 0) return BatchTransferStatus.COMPLETED;
        if (success == 0) return BatchTransferStatus.FAILED;
        return BatchTransferStatus.PARTIALLY_COMPLETED;
    }
}


package com.datdevops.hamariadb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.datdevops.hamariadb.dto.request.BatchTransferRequest;
import com.datdevops.hamariadb.dto.request.TransferRequest;
import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.dto.response.BatchTransferResponse;
import com.datdevops.hamariadb.dto.response.TransferResponse;
import com.datdevops.hamariadb.service.tranfer.BatchTransferService;
import com.datdevops.hamariadb.service.tranfer.TransferService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller chính xử lý các loại giao dịch chuyển tiền khác nhau.
 * Đóng vai trò là một điểm vào chung cho các chức năng chuyển tiền.
 *
 * @deprecated Cân nhắc tách các chức năng ra các controller riêng biệt
 * (ví dụ: `SingleTransferController`, `BatchTransferController`) để mã nguồn rõ ràng hơn.
 * Hiện tại, các endpoint đã được tách ra `BatchTransferController`, `RecurringTransferController`,
 * và `ScheduledTransferController`. Lớp này có thể được giữ lại cho các giao dịch đơn lẻ
 * hoặc được tái cấu trúc.
 */
@Slf4j
@RestController
@RequestMapping("/v1/transfers")
public class TransferController {

    private final TransferService transferService;
    private final BatchTransferService batchTransferService;

    public TransferController(TransferService transferService,
                              BatchTransferService batchTransferService) {
        this.transferService = transferService;
        this.batchTransferService = batchTransferService;
    }

    /**
     * Endpoint để thực hiện một giao dịch chuyển tiền đơn lẻ.
     * @param request Dữ liệu yêu cầu chuyển tiền.
     * @param authentication Thông tin xác thực của người dùng.
     * @return ResponseEntity chứa kết quả giao dịch.
     */
    @PostMapping("/single")
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Creating transfer for user: {}", username);

        TransferResponse response = transferService.createTransfer(request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Transfer initiated successfully"));
    }

    /**
     * Endpoint để thực hiện chuyển tiền theo lô.
     * @deprecated Endpoint này đã được chuyển sang {@link BatchTransferController}.
     * Giữ lại ở đây có thể gây nhầm lẫn.
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<BatchTransferResponse>> createBatchTransfer(
            @RequestPart("request") BatchTransferRequest request,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Processing batch transfer for user: {}, file: {}", username, file.getOriginalFilename());

        BatchTransferResponse response = batchTransferService.processBatchTransfer(request, file, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Batch transfer submitted for processing"));
    }

   /*
    * Endpoint để tạo giao dịch theo lịch (đã được comment).
    * @deprecated Chức năng này đã được triển khai trong {@link ScheduledTransferController}.
    *
    @PostMapping("/scheduled")
    public ResponseEntity<ApiResponse<TransferResponse>> createScheduledTransfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Creating scheduled transfer for user: {}", username);

        // Implementation for scheduled transfers
        // This would be similar to single transfer but with scheduling logic

        TransferResponse response = TransferResponse.builder()
                .transferId("sch_" + System.currentTimeMillis())
                .transferReference("SCHED_" + System.currentTimeMillis())
                .status(TransferStatus.PENDING)
                .message("Scheduled transfer created successfully")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Scheduled transfer created successfully"));
    }*/
}

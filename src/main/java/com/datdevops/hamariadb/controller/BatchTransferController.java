package com.datdevops.hamariadb.controller;


import com.datdevops.hamariadb.dto.request.BatchTransferRequest;
import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.dto.response.BatchTransferResponse;
import com.datdevops.hamariadb.service.tranfer.BatchTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller xử lý các yêu cầu liên quan đến chuyển tiền theo lô (batch transfer).
 */
@Slf4j
@RestController
@RequestMapping("/v1/transfers/batch")
public class BatchTransferController {

    private final BatchTransferService batchTransferService;

    public BatchTransferController(BatchTransferService batchTransferService) {
        this.batchTransferService = batchTransferService;
    }

    /**
     * Endpoint để tạo và xử lý một giao dịch chuyển tiền theo lô từ file.
     * Yêu cầu một request multipart/form-data chứa cả dữ liệu JSON và file.
     * @param request Dữ liệu JSON chứa thông tin chung của lô (ví dụ: kênh chuyển tiền).
     * @param file File Excel chứa danh sách các giao dịch chi tiết.
     * @param authentication Thông tin xác thực của người dùng.
     * @return ResponseEntity chứa thông tin ban đầu của lô sau khi được gửi đi để xử lý.
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<BatchTransferResponse>> createBatchTransfer(
            @RequestPart("request") BatchTransferRequest request,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Processing batch transfer for user: {}, file: {}", username, file.getOriginalFilename());

        BatchTransferResponse response = batchTransferService.processBatchTransfer(request, file, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Batch transfer submitted for processing"));
    }

    /**
     * Endpoint để lấy thông tin chi tiết của một lô giao dịch.
     * @param batchId ID của lô cần truy vấn.
     * @param authentication Thông tin xác thực của người dùng.
     * @return ResponseEntity chứa thông tin chi tiết của lô.
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<ApiResponse<BatchTransferResponse>> getBatchTransfer(
            @PathVariable String batchId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get batch transfer: {} by user: {}", batchId, username);

        BatchTransferResponse response = batchTransferService.getBatchTransfer(batchId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Batch transfer retrieved successfully"));
    }

    /**
     * Endpoint để lấy danh sách các lô giao dịch của người dùng đang đăng nhập.
     * @param authentication Thông tin xác thực của người dùng.
     * @return ResponseEntity chứa danh sách các lô giao dịch.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BatchTransferResponse>>> getBatchTransfers(
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get batch transfers for user: {}", username);

        List<BatchTransferResponse> responses = batchTransferService.getBatchTransfers(username);
        return ResponseEntity.ok(ApiResponse.success(responses, "Batch transfers retrieved successfully"));
    }
}

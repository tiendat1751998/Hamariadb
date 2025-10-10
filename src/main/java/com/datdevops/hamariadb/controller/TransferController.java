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

    @PostMapping("/single")
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Creating transfer for user: {}", username);

        TransferResponse response = transferService.createTransfer(request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Transfer initiated successfully"));
    }

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

   /*  @PostMapping("/scheduled")
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

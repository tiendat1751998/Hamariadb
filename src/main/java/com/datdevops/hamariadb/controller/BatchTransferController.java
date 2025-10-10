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

@Slf4j
@RestController
@RequestMapping("/v1/transfers/batch")
public class BatchTransferController {

    private final BatchTransferService batchTransferService;

    public BatchTransferController(BatchTransferService batchTransferService) {
        this.batchTransferService = batchTransferService;
    }

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

    @GetMapping("/{batchId}")
    public ResponseEntity<ApiResponse<BatchTransferResponse>> getBatchTransfer(
            @PathVariable String batchId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get batch transfer: {} by user: {}", batchId, username);

        BatchTransferResponse response = batchTransferService.getBatchTransfer(batchId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Batch transfer retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BatchTransferResponse>>> getBatchTransfers(
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get batch transfers for user: {}", username);

        List<BatchTransferResponse> responses = batchTransferService.getBatchTransfers(username);
        return ResponseEntity.ok(ApiResponse.success(responses, "Batch transfers retrieved successfully"));
    }
}

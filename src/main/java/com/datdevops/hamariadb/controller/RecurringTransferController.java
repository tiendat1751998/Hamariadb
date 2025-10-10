package com.datdevops.hamariadb.controller;


import com.datdevops.hamariadb.dto.request.RecurringTransferRequest;
import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.dto.response.RecurringTransferResponse;
import com.datdevops.hamariadb.service.tranfer.RecurringTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/transfers/recurring")
public class RecurringTransferController {

    private final RecurringTransferService recurringTransferService;

    public RecurringTransferController(RecurringTransferService recurringTransferService) {
        this.recurringTransferService = recurringTransferService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringTransferResponse>> createRecurringTransfer(
            @RequestBody RecurringTransferRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Creating recurring transfer for user: {}", username);

        RecurringTransferResponse response = recurringTransferService.createRecurringTransfer(request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Recurring transfer created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringTransferResponse>>> getRecurringTransfers(
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get recurring transfers for user: {}", username);

        List<RecurringTransferResponse> responses = recurringTransferService.getRecurringTransfers(username);
        return ResponseEntity.ok(ApiResponse.success(responses, "Recurring transfers retrieved successfully"));
    }

    @PutMapping("/{recurringId}")
    public ResponseEntity<ApiResponse<RecurringTransferResponse>> updateRecurringTransfer(
            @PathVariable String recurringId,
            @RequestBody RecurringTransferRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Updating recurring transfer {} for user: {}", recurringId, username);

        RecurringTransferResponse response = recurringTransferService.updateRecurringTransfer(recurringId, request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Recurring transfer updated successfully"));
    }

    @DeleteMapping("/{recurringId}")
    public ResponseEntity<ApiResponse<Void>> cancelRecurringTransfer(
            @PathVariable String recurringId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Canceling recurring transfer {} for user: {}", recurringId, username);

        recurringTransferService.cancelRecurringTransfer(recurringId, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Recurring transfer canceled successfully"));
    }

    @PostMapping("/{recurringId}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseRecurringTransfer(
            @PathVariable String recurringId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Pausing recurring transfer {} for user: {}", recurringId, username);

        recurringTransferService.pauseRecurringTransfer(recurringId, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Recurring transfer paused successfully"));
    }

    @PostMapping("/{recurringId}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeRecurringTransfer(
            @PathVariable String recurringId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Resuming recurring transfer {} for user: {}", recurringId, username);

        recurringTransferService.resumeRecurringTransfer(recurringId, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Recurring transfer resumed successfully"));
    }
}

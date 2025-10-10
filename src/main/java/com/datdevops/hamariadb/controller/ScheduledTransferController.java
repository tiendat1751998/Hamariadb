package com.datdevops.hamariadb.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datdevops.hamariadb.dto.request.ScheduledTransferRequest;
import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.dto.response.ScheduledTransferResponse;
import com.datdevops.hamariadb.service.tranfer.ScheduledTransferService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/transfers/scheduled")
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;

    public ScheduledTransferController(ScheduledTransferService scheduledTransferService) {
        this.scheduledTransferService = scheduledTransferService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledTransferResponse>> createScheduledTransfer(
            @RequestBody ScheduledTransferRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Creating scheduled transfer for user: {}", username);

        ScheduledTransferResponse response = scheduledTransferService.createScheduledTransfer(request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Scheduled transfer created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledTransferResponse>>> getScheduledTransfers(
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get scheduled transfers for user: {}", username);

        List<ScheduledTransferResponse> responses = scheduledTransferService.getScheduledTransfers(username);
        return ResponseEntity.ok(ApiResponse.success(responses, "Scheduled transfers retrieved successfully"));
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduledTransferResponse>> updateScheduledTransfer(
            @PathVariable String scheduleId,
            @RequestBody ScheduledTransferRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Updating scheduled transfer {} for user: {}", scheduleId, username);

        ScheduledTransferResponse response = scheduledTransferService.updateScheduledTransfer(scheduleId, request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Scheduled transfer updated successfully"));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> cancelScheduledTransfer(
            @PathVariable String scheduleId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Canceling scheduled transfer {} for user: {}", scheduleId, username);

        scheduledTransferService.cancelScheduledTransfer(scheduleId, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Scheduled transfer canceled successfully"));
    }
}

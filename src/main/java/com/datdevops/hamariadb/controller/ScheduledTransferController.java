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

/**
 * Controller (bộ điều khiển) xử lý các yêu cầu HTTP liên quan đến chuyển tiền theo lịch.
 * Cung cấp các endpoint để tạo, lấy, cập nhật và hủy các giao dịch đã được lên lịch.
 */
@Slf4j
@RestController
@RequestMapping("/v1/transfers/scheduled") // Tất cả các endpoint trong controller này sẽ có tiền tố là /v1/transfers/scheduled
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;

    public ScheduledTransferController(ScheduledTransferService scheduledTransferService) {
        this.scheduledTransferService = scheduledTransferService;
    }

    /**
     * Endpoint để tạo một giao dịch chuyển tiền mới theo lịch.
     * HTTP Method: POST
     * URL: /v1/transfers/scheduled
     * @param request Dữ liệu yêu cầu từ client, chứa thông tin về giao dịch cần lên lịch.
     * @param authentication Đối tượng chứa thông tin xác thực của người dùng đang đăng nhập.
     * @return ResponseEntity chứa kết quả của việc tạo lịch.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledTransferResponse>> createScheduledTransfer(
            @RequestBody ScheduledTransferRequest request,
            Authentication authentication) {

        String username = authentication.getName(); // Lấy username của người dùng đã xác thực
        log.info("Creating scheduled transfer for user: {}", username);

        // Gọi service để xử lý logic tạo lịch
        ScheduledTransferResponse response = scheduledTransferService.createScheduledTransfer(request, username);
        // Trả về kết quả thành công (HTTP 200 OK)
        return ResponseEntity.ok(ApiResponse.success(response, "Scheduled transfer created successfully"));
    }

    /**
     * Endpoint để lấy danh sách các giao dịch đã được lên lịch của người dùng.
     * HTTP Method: GET
     * URL: /v1/transfers/scheduled
     * @param authentication Đối tượng chứa thông tin xác thực của người dùng.
     * @return ResponseEntity chứa danh sách các giao dịch đã lên lịch.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledTransferResponse>>> getScheduledTransfers(
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get scheduled transfers for user: {}", username);

        // Gọi service để lấy danh sách
        List<ScheduledTransferResponse> responses = scheduledTransferService.getScheduledTransfers(username);
        return ResponseEntity.ok(ApiResponse.success(responses, "Scheduled transfers retrieved successfully"));
    }

    /**
     * Endpoint để cập nhật một giao dịch đã được lên lịch.
     * HTTP Method: PUT
     * URL: /v1/transfers/scheduled/{scheduleId}
     * @param scheduleId ID của lịch cần cập nhật (lấy từ URL).
     * @param request Dữ liệu cập nhật từ client.
     * @param authentication Đối tượng chứa thông tin xác thực của người dùng.
     * @return ResponseEntity chứa thông tin giao dịch sau khi đã cập nhật.
     */
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduledTransferResponse>> updateScheduledTransfer(
            @PathVariable String scheduleId,
            @RequestBody ScheduledTransferRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Updating scheduled transfer {} for user: {}", scheduleId, username);

        // Gọi service để xử lý logic cập nhật
        ScheduledTransferResponse response = scheduledTransferService.updateScheduledTransfer(scheduleId, request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Scheduled transfer updated successfully"));
    }

    /**
     * Endpoint để hủy một giao dịch đã được lên lịch.
     * HTTP Method: DELETE
     * URL: /v1/transfers/scheduled/{scheduleId}
     * @param scheduleId ID của lịch cần hủy (lấy từ URL).
     * @param authentication Đối tượng chứa thông tin xác thực của người dùng.
     * @return ResponseEntity xác nhận việc hủy đã thành công.
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> cancelScheduledTransfer(
            @PathVariable String scheduleId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Canceling scheduled transfer {} for user: {}", scheduleId, username);

        // Gọi service để xử lý logic hủy
        scheduledTransferService.cancelScheduledTransfer(scheduleId, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Scheduled transfer canceled successfully"));
    }
}

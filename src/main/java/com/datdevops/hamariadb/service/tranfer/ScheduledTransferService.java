package com.datdevops.hamariadb.service.tranfer;


import com.datdevops.hamariadb.dto.request.ScheduledTransferRequest;
import com.datdevops.hamariadb.dto.response.ScheduledTransferResponse;
import com.datdevops.hamariadb.entity.Account;
import com.datdevops.hamariadb.entity.ScheduledTransfer;
import com.datdevops.hamariadb.entity.ScheduledTransferStatus;
import com.datdevops.hamariadb.entity.User;
import com.datdevops.hamariadb.repository.dao.AccountRepository;
import com.datdevops.hamariadb.repository.dao.ScheduledTransferRepository;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.repository.mapper.EntityMapper;
import com.datdevops.hamariadb.service.account.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service để quản lý các giao dịch chuyển tiền được lên lịch.
 */
@Slf4j
@Service
@Transactional
public class ScheduledTransferService {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final TransferService transferService;
    private final EntityMapper entityMapper;

    /**
     * Constructor để inject các dependency cần thiết.
     */
    public ScheduledTransferService(ScheduledTransferRepository scheduledTransferRepository,
                                    AccountRepository accountRepository,
                                    UserRepository userRepository,
                                    TransactionService transactionService,
                                    TransferService transferService, EntityMapper entityMapper) {
        this.scheduledTransferRepository = scheduledTransferRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.transferService = transferService;
        this.entityMapper = entityMapper;
    }

    /**
     * Tạo một giao dịch chuyển tiền được lên lịch mới.
     * @param request Dữ liệu yêu cầu tạo lịch.
     * @param username Tên người dùng tạo lịch.
     * @return Phản hồi chứa thông tin của giao dịch đã được lên lịch.
     */
    public ScheduledTransferResponse createScheduledTransfer(ScheduledTransferRequest request, String username) {
        // Tìm người dùng theo username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tìm tài khoản nguồn
        Account fromAccount = accountRepository.findByAccountNumberWithUser(request.getFromAccount())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        // Xác thực người dùng sở hữu tài khoản nguồn
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own the source account");
        }

        // Xác thực thời gian thực thi phải ở trong tương lai
        if (request.getExecuteAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Execution time must be in the future");
        }

        // Tạo đối tượng ScheduledTransfer mới
        ScheduledTransfer scheduledTransfer = new ScheduledTransfer();
        scheduledTransfer.setScheduleId(generateScheduleId());
        scheduledTransfer.setFromAccount(fromAccount);
        scheduledTransfer.setToAccountNumber(request.getToAccount());
        scheduledTransfer.setToBankCode(request.getToBankCode());
        scheduledTransfer.setAmount(request.getAmount());
        scheduledTransfer.setDescription(request.getDescription());
        scheduledTransfer.setExecuteAt(request.getExecuteAt());
        scheduledTransfer.setStatus(ScheduledTransferStatus.SCHEDULED);
        scheduledTransfer.setCreatedBy(user);

        // Lưu vào cơ sở dữ liệu
        scheduledTransfer = scheduledTransferRepository.save(scheduledTransfer);

        log.info("Scheduled transfer created: {} for user: {}", scheduledTransfer.getScheduleId(), username);

        // Chuyển đổi entity thành DTO để trả về
        return entityMapper.toScheduledTransferResponse(scheduledTransfer);
    }

    /**
     * Lấy danh sách các giao dịch đã được lên lịch của một người dùng.
     * @param username Tên người dùng.
     * @return Danh sách các giao dịch đã lên lịch.
     */
    public List<ScheduledTransferResponse> getScheduledTransfers(String username) {
        List<ScheduledTransfer> scheduledTransfers = scheduledTransferRepository.findByUsername(username);

        // Chuyển đổi danh sách entity thành danh sách DTO
        return scheduledTransfers.stream()
                .map(entityMapper::toScheduledTransferResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật một giao dịch đã được lên lịch.
     * @param scheduleId ID của lịch.
     * @param request Dữ liệu cập nhật.
     * @param username Tên người dùng thực hiện.
     * @return Phản hồi chứa thông tin của giao dịch sau khi cập nhật.
     */
    public ScheduledTransferResponse updateScheduledTransfer(String scheduleId, ScheduledTransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new RuntimeException("Scheduled transfer not found"));

        // Xác thực người dùng sở hữu lịch chuyển tiền này
        if (!scheduledTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this scheduled transfer");
        }

        // Chỉ có thể cập nhật nếu lịch chưa được thực thi hoặc hủy
        if (scheduledTransfer.getStatus() != ScheduledTransferStatus.SCHEDULED) {
            throw new RuntimeException("Cannot update executed or cancelled transfer");
        }

        // Cập nhật các trường thông tin
        scheduledTransfer.setToAccountNumber(request.getToAccount());
        scheduledTransfer.setToBankCode(request.getToBankCode());
        scheduledTransfer.setAmount(request.getAmount());
        scheduledTransfer.setDescription(request.getDescription());
        scheduledTransfer.setExecuteAt(request.getExecuteAt());

        scheduledTransfer = scheduledTransferRepository.save(scheduledTransfer);

        log.info("Scheduled transfer updated: {} for user: {}", scheduleId, username);

        return entityMapper.toScheduledTransferResponse(scheduledTransfer);
    }

    /**
     * Hủy một giao dịch đã được lên lịch.
     * @param scheduleId ID của lịch.
     * @param username Tên người dùng thực hiện.
     */
    public void cancelScheduledTransfer(String scheduleId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new RuntimeException("Scheduled transfer not found"));

        // Xác thực người dùng sở hữu lịch
        if (!scheduledTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this scheduled transfer");
        }

        // Chỉ có thể hủy nếu lịch chưa được thực thi hoặc đã hủy
        if (scheduledTransfer.getStatus() != ScheduledTransferStatus.SCHEDULED) {
            throw new RuntimeException("Cannot cancel executed or already cancelled transfer");
        }

        // Cập nhật trạng thái thành CANCELLED
        scheduledTransfer.setStatus(ScheduledTransferStatus.CANCELLED);
        scheduledTransferRepository.save(scheduledTransfer);

        log.info("Scheduled transfer cancelled: {} for user: {}", scheduleId, username);
    }

    /**
     * Xử lý các giao dịch đến hạn thực thi (thường được gọi bởi một scheduler).
     */
    @Transactional
    public void processDueScheduledTransfers() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledTransfer> dueTransfers = scheduledTransferRepository.findDueScheduledTransfers(now);

        log.info("Processing {} due scheduled transfers", dueTransfers.size());

        for (ScheduledTransfer scheduledTransfer : dueTransfers) {
            try {
                processSingleScheduledTransfer(scheduledTransfer);
            } catch (Exception e) {
                log.error("Failed to process scheduled transfer: {}", scheduledTransfer.getScheduleId(), e);
                // Nếu xử lý thất bại, cập nhật trạng thái thành FAILED
                scheduledTransfer.setStatus(ScheduledTransferStatus.FAILED);
                scheduledTransferRepository.save(scheduledTransfer);
            }
        }
    }

    /**
     * Xử lý một giao dịch đơn lẻ đến hạn.
     * @param scheduledTransfer Giao dịch cần xử lý.
     */
    private void processSingleScheduledTransfer(ScheduledTransfer scheduledTransfer) {
        // TODO: Logic thực sự để thực hiện chuyển tiền.
        // Cần tạo một yêu cầu TransferRequest từ scheduledTransfer
        // và gọi transferService.createTransfer(transferRequest, username).
        // Hiện tại, chỉ giả lập việc xử lý thành công.

        scheduledTransfer.setStatus(ScheduledTransferStatus.EXECUTED);
        scheduledTransfer.setExecutedAt(LocalDateTime.now());
        scheduledTransferRepository.save(scheduledTransfer);

        log.info("Scheduled transfer executed: {}", scheduledTransfer.getScheduleId());
    }

    /**
     * Tạo một ID duy nhất cho lịch.
     * @return ID của lịch.
     */
    private String generateScheduleId() {
        return "SCH_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * Chuyển đổi một đối tượng ScheduledTransfer (Entity) thành ScheduledTransferResponse (DTO).
     * @param scheduledTransfer Đối tượng Entity.
     * @return Đối tượng DTO.
     */
    private ScheduledTransferResponse convertToResponse(ScheduledTransfer scheduledTransfer) {
        return ScheduledTransferResponse.builder()
                .scheduleId(scheduledTransfer.getScheduleId())
                .fromAccount(scheduledTransfer.getFromAccount().getAccountNumber())
                .toAccount(scheduledTransfer.getToAccountNumber())
                .toBankCode(scheduledTransfer.getToBankCode())
                .amount(scheduledTransfer.getAmount())
                .description(scheduledTransfer.getDescription())
                .executeAt(scheduledTransfer.getExecuteAt())
                .status(scheduledTransfer.getStatus())
                .createdAt(scheduledTransfer.getCreatedAt())
                .build();
    }
}

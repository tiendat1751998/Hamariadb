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

    public ScheduledTransferResponse createScheduledTransfer(ScheduledTransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account fromAccount = accountRepository.findByAccountNumberWithUser(request.getFromAccount())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        // Verify user owns the account
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own the source account");
        }

        // Validate execution time (must be in the future)
        if (request.getExecuteAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Execution time must be in the future");
        }

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

        scheduledTransfer = scheduledTransferRepository.save(scheduledTransfer);

        log.info("Scheduled transfer created: {} for user: {}", scheduledTransfer.getScheduleId(), username);

        return EntityMapper.toScheduledTransferResponse(scheduledTransfer);
    }

    public List<ScheduledTransferResponse> getScheduledTransfers(String username) {
        List<ScheduledTransfer> scheduledTransfers = scheduledTransferRepository.findByUsername(username);

        return scheduledTransfers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ScheduledTransferResponse updateScheduledTransfer(String scheduleId, ScheduledTransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new RuntimeException("Scheduled transfer not found"));

        // Verify user owns the scheduled transfer
        if (!scheduledTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this scheduled transfer");
        }

        // Can only update if not yet executed
        if (scheduledTransfer.getStatus() != ScheduledTransferStatus.SCHEDULED) {
            throw new RuntimeException("Cannot update executed or cancelled transfer");
        }

        // Update fields
        scheduledTransfer.setToAccountNumber(request.getToAccount());
        scheduledTransfer.setToBankCode(request.getToBankCode());
        scheduledTransfer.setAmount(request.getAmount());
        scheduledTransfer.setDescription(request.getDescription());
        scheduledTransfer.setExecuteAt(request.getExecuteAt());

        scheduledTransfer = scheduledTransferRepository.save(scheduledTransfer);

        log.info("Scheduled transfer updated: {} for user: {}", scheduleId, username);

        return entityMapper.toScheduledTransferResponse(scheduledTransfer);
    }

    public void cancelScheduledTransfer(String scheduleId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new RuntimeException("Scheduled transfer not found"));

        // Verify user owns the scheduled transfer
        if (!scheduledTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this scheduled transfer");
        }

        // Can only cancel if not yet executed
        if (scheduledTransfer.getStatus() != ScheduledTransferStatus.SCHEDULED) {
            throw new RuntimeException("Cannot cancel executed or already cancelled transfer");
        }

        scheduledTransfer.setStatus(ScheduledTransferStatus.CANCELLED);
        scheduledTransferRepository.save(scheduledTransfer);

        log.info("Scheduled transfer cancelled: {} for user: {}", scheduleId, username);
    }

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
                scheduledTransfer.setStatus(ScheduledTransferStatus.FAILED);
                scheduledTransferRepository.save(scheduledTransfer);
            }
        }
    }

    private void processSingleScheduledTransfer(ScheduledTransfer scheduledTransfer) {
        // Create transfer request from scheduled transfer
        // This would typically call the main transfer service
        // For now, we'll simulate the processing

        scheduledTransfer.setStatus(ScheduledTransferStatus.EXECUTED);
        scheduledTransfer.setExecutedAt(LocalDateTime.now());
        scheduledTransferRepository.save(scheduledTransfer);

        log.info("Scheduled transfer executed: {}", scheduledTransfer.getScheduleId());
    }

    private String generateScheduleId() {
        return "SCH_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

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

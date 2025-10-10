package com.datdevops.hamariadb.service.tranfer;


import com.datdevops.hamariadb.dto.request.RecurringTransferRequest;
import com.datdevops.hamariadb.dto.response.RecurringTransferResponse;
import com.datdevops.hamariadb.entity.*;
import com.datdevops.hamariadb.repository.dao.AccountRepository;
import com.datdevops.hamariadb.repository.dao.RecurringTransferRepository;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.repository.mapper.EntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class RecurringTransferService {

    private final RecurringTransferRepository recurringTransferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransferService transferService;
    private final EntityMapper entityMapper;

    public RecurringTransferService(RecurringTransferRepository recurringTransferRepository,
                                    AccountRepository accountRepository,
                                    UserRepository userRepository,
                                    TransferService transferService, EntityMapper entityMapper) {
        this.recurringTransferRepository = recurringTransferRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transferService = transferService;
        this.entityMapper = entityMapper;
    }

    public RecurringTransferResponse createRecurringTransfer(RecurringTransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account fromAccount = accountRepository.findByAccountNumberWithUser(request.getFromAccount())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        // Verify user owns the account
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own the source account");
        }

        // Validate dates
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date must be in the future");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        RecurringTransfer recurringTransfer = new RecurringTransfer();
        recurringTransfer.setRecurringId(generateRecurringId());
        recurringTransfer.setFromAccount(fromAccount);
        recurringTransfer.setToAccountNumber(request.getToAccount());
        recurringTransfer.setToBankCode(request.getToBankCode());
        recurringTransfer.setAmount(request.getAmount());
        recurringTransfer.setDescription(request.getDescription());
        recurringTransfer.setFrequency(Frequency.valueOf(request.getFrequency()));
        recurringTransfer.setStartDate(request.getStartDate());
        recurringTransfer.setEndDate(request.getEndDate());
        recurringTransfer.setNextExecutionDate(request.getStartDate());
        recurringTransfer.setStatus(RecurringTransferStatus.ACTIVE);
        recurringTransfer.setTotalOccurrences(request.getTotalOccurrences());
        recurringTransfer.setExecutedOccurrences(0);
        recurringTransfer.setCreatedBy(user);

        recurringTransfer = recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer created: {} for user: {}", recurringTransfer.getRecurringId(), username);

        return EntityMapper.toRecurringTransferResponse(recurringTransfer);
    }

    public List<RecurringTransferResponse> getRecurringTransfers(String username) {
        List<RecurringTransfer> recurringTransfers = recurringTransferRepository.findByUsername(username);

        return recurringTransfers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public RecurringTransferResponse updateRecurringTransfer(String recurringId, RecurringTransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transfer not found"));

        // Verify user owns the recurring transfer
        if (!recurringTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this recurring transfer");
        }

        // Can only update if active
        if (recurringTransfer.getStatus() != RecurringTransferStatus.ACTIVE) {
            throw new RuntimeException("Cannot update inactive recurring transfer");
        }

        // Update fields
        recurringTransfer.setToAccountNumber(request.getToAccount());
        recurringTransfer.setToBankCode(request.getToBankCode());
        recurringTransfer.setAmount(request.getAmount());
        recurringTransfer.setDescription(request.getDescription());
        recurringTransfer.setFrequency(Frequency.valueOf(request.getFrequency()));
        recurringTransfer.setStartDate(request.getStartDate());
        recurringTransfer.setEndDate(request.getEndDate());
        recurringTransfer.setTotalOccurrences(request.getTotalOccurrences());

        recurringTransfer = recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer updated: {} for user: {}", recurringId, username);

        return entityMapper.toRecurringTransferResponse(recurringTransfer);
    }

    public void cancelRecurringTransfer(String recurringId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transfer not found"));

        // Verify user owns the recurring transfer
        if (!recurringTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this recurring transfer");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.CANCELLED);
        recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer cancelled: {} for user: {}", recurringId, username);
    }

    public void pauseRecurringTransfer(String recurringId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transfer not found"));

        // Verify user owns the recurring transfer
        if (!recurringTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this recurring transfer");
        }

        if (recurringTransfer.getStatus() != RecurringTransferStatus.ACTIVE) {
            throw new RuntimeException("Only active recurring transfers can be paused");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.PAUSED);
        recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer paused: {} for user: {}", recurringId, username);
    }

    public void resumeRecurringTransfer(String recurringId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transfer not found"));

        // Verify user owns the recurring transfer
        if (!recurringTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this recurring transfer");
        }

        if (recurringTransfer.getStatus() != RecurringTransferStatus.PAUSED) {
            throw new RuntimeException("Only paused recurring transfers can be resumed");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.ACTIVE);
        recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer resumed: {} for user: {}", recurringId, username);
    }

    @Transactional
    public void processDueRecurringTransfers() {
        LocalDate today = LocalDate.now();
        List<RecurringTransfer> dueTransfers = recurringTransferRepository.findDueRecurringTransfers(today);

        log.info("Processing {} due recurring transfers", dueTransfers.size());

        for (RecurringTransfer recurringTransfer : dueTransfers) {
            try {
                processSingleRecurringTransfer(recurringTransfer);
            } catch (Exception e) {
                log.error("Failed to process recurring transfer: {}", recurringTransfer.getRecurringId(), e);
                // Don't change status on failure - it will retry next day
            }
        }
    }

    private void processSingleRecurringTransfer(RecurringTransfer recurringTransfer) {
        // Update next execution date
        updateNextExecutionDate(recurringTransfer);

        // Increment executed occurrences
        recurringTransfer.setExecutedOccurrences(recurringTransfer.getExecutedOccurrences() + 1);

        // Check if completed
        if (recurringTransfer.getTotalOccurrences() != null &&
                recurringTransfer.getExecutedOccurrences() >= recurringTransfer.getTotalOccurrences()) {
            recurringTransfer.setStatus(RecurringTransferStatus.COMPLETED);
        }

        // Check if end date reached
        if (recurringTransfer.getEndDate() != null &&
                recurringTransfer.getNextExecutionDate().isAfter(recurringTransfer.getEndDate())) {
            recurringTransfer.setStatus(RecurringTransferStatus.COMPLETED);
        }

        recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer processed: {}", recurringTransfer.getRecurringId());
    }

    private void updateNextExecutionDate(RecurringTransfer recurringTransfer) {
        LocalDate nextDate = recurringTransfer.getNextExecutionDate();

        switch (recurringTransfer.getFrequency()) {
            case DAILY:
                nextDate = nextDate.plusDays(1);
                break;
            case WEEKLY:
                nextDate = nextDate.plusWeeks(1);
                break;
            case MONTHLY:
                nextDate = nextDate.plusMonths(1);
                break;
            case QUARTERLY:
                nextDate = nextDate.plusMonths(3);
                break;
            case YEARLY:
                nextDate = nextDate.plusYears(1);
                break;
        }

        recurringTransfer.setNextExecutionDate(nextDate);
    }

    private String generateRecurringId() {
        return "REC_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private RecurringTransferResponse convertToResponse(RecurringTransfer recurringTransfer) {
        return RecurringTransferResponse.builder()
                .recurringId(recurringTransfer.getRecurringId())
                .fromAccount(recurringTransfer.getFromAccount().getAccountNumber())
                .toAccount(recurringTransfer.getToAccountNumber())
                .toBankCode(recurringTransfer.getToBankCode())
                .amount(recurringTransfer.getAmount())
                .description(recurringTransfer.getDescription())
                .frequency(recurringTransfer.getFrequency())
                .startDate(recurringTransfer.getStartDate())
                .endDate(recurringTransfer.getEndDate())
                .nextExecutionDate(recurringTransfer.getNextExecutionDate())
                .status(recurringTransfer.getStatus())
                .totalOccurrences(recurringTransfer.getTotalOccurrences())
                .executedOccurrences(recurringTransfer.getExecutedOccurrences())
                .createdAt(recurringTransfer.getCreatedAt())
                .build();
    }
}

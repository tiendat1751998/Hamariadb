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

/**
 * Service để quản lý các giao dịch chuyển tiền định kỳ.
 */
@Slf4j
@Service
@Transactional
public class RecurringTransferService {

    private final RecurringTransferRepository recurringTransferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransferService transferService;
    private final EntityMapper entityMapper;

    /**
     * Constructor để inject các dependency.
     */
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

    /**
     * Tạo một giao dịch chuyển tiền định kỳ mới.
     * @param request Dữ liệu yêu cầu tạo lịch định kỳ.
     * @param username Tên người dùng tạo.
     * @return Phản hồi chứa thông tin của giao dịch định kỳ đã tạo.
     */
    public RecurringTransferResponse createRecurringTransfer(RecurringTransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account fromAccount = accountRepository.findByAccountNumberWithUser(request.getFromAccount())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        // Xác thực người dùng sở hữu tài khoản nguồn
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own the source account");
        }

        // Xác thực ngày bắt đầu phải trong tương lai
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date must be in the future");
        }

        // Xác thực ngày kết thúc (nếu có) phải sau ngày bắt đầu
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        // Tạo đối tượng RecurringTransfer mới
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
        recurringTransfer.setNextExecutionDate(request.getStartDate()); // Ngày thực thi đầu tiên là ngày bắt đầu
        recurringTransfer.setStatus(RecurringTransferStatus.ACTIVE);
        recurringTransfer.setTotalOccurrences(request.getTotalOccurrences());
        recurringTransfer.setExecutedOccurrences(0);
        recurringTransfer.setCreatedBy(user);

        recurringTransfer = recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer created: {} for user: {}", recurringTransfer.getRecurringId(), username);

        return entityMapper.toRecurringTransferResponse(recurringTransfer);
    }

    /**
     * Lấy danh sách các giao dịch định kỳ của một người dùng.
     * @param username Tên người dùng.
     * @return Danh sách các giao dịch định kỳ.
     */
    public List<RecurringTransferResponse> getRecurringTransfers(String username) {
        List<RecurringTransfer> recurringTransfers = recurringTransferRepository.findByUsername(username);

        return recurringTransfers.stream()
                .map(entityMapper::toRecurringTransferResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật một giao dịch định kỳ.
     * @param recurringId ID của giao dịch định kỳ.
     * @param request Dữ liệu cập nhật.
     * @param username Tên người dùng thực hiện.
     * @return Phản hồi chứa thông tin sau khi cập nhật.
     */
    public RecurringTransferResponse updateRecurringTransfer(String recurringId, RecurringTransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transfer not found"));

        // Xác thực quyền sở hữu
        if (!recurringTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this recurring transfer");
        }

        // Chỉ cập nhật khi đang ở trạng thái ACTIVE
        if (recurringTransfer.getStatus() != RecurringTransferStatus.ACTIVE) {
            throw new RuntimeException("Cannot update inactive recurring transfer");
        }

        // Cập nhật các trường
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

    /**
     * Hủy một giao dịch định kỳ.
     * @param recurringId ID của giao dịch định kỳ.
     * @param username Tên người dùng thực hiện.
     */
    public void cancelRecurringTransfer(String recurringId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transfer not found"));

        // Xác thực quyền sở hữu
        if (!recurringTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this recurring transfer");
        }

        // Chuyển trạng thái thành CANCELLED
        recurringTransfer.setStatus(RecurringTransferStatus.CANCELLED);
        recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer cancelled: {} for user: {}", recurringId, username);
    }

    /**
     * Tạm dừng một giao dịch định kỳ.
     * @param recurringId ID của giao dịch định kỳ.
     * @param username Tên người dùng thực hiện.
     */
    public void pauseRecurringTransfer(String recurringId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transfer not found"));

        // Xác thực quyền sở hữu
        if (!recurringTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this recurring transfer");
        }

        // Chỉ tạm dừng khi đang ACTIVE
        if (recurringTransfer.getStatus() != RecurringTransferStatus.ACTIVE) {
            throw new RuntimeException("Only active recurring transfers can be paused");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.PAUSED);
        recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer paused: {} for user: {}", recurringId, username);
    }

    /**
     * Tiếp tục một giao dịch định kỳ đã tạm dừng.
     * @param recurringId ID của giao dịch định kỳ.
     * @param username Tên người dùng thực hiện.
     */
    public void resumeRecurringTransfer(String recurringId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transfer not found"));

        // Xác thực quyền sở hữu
        if (!recurringTransfer.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this recurring transfer");
        }

        // Chỉ tiếp tục khi đang PAUSED
        if (recurringTransfer.getStatus() != RecurringTransferStatus.PAUSED) {
            throw new RuntimeException("Only paused recurring transfers can be resumed");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.ACTIVE);
        recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer resumed: {} for user: {}", recurringId, username);
    }

    /**
     * Xử lý các giao dịch định kỳ đến hạn (thường được gọi bởi scheduler).
     */
    @Transactional
    public void processDueRecurringTransfers() {
        LocalDate today = LocalDate.now();
        List<RecurringTransfer> dueTransfers = recurringTransferRepository.findDueRecurringTransfers(today);

        log.info("Processing {} due recurring transfers", dueTransfers.size());

        for (RecurringTransfer recurringTransfer : dueTransfers) {
            try {
                // TODO: Thực hiện chuyển tiền thực sự ở đây bằng cách gọi transferService
                processSingleRecurringTransfer(recurringTransfer);
            } catch (Exception e) {
                log.error("Failed to process recurring transfer: {}", recurringTransfer.getRecurringId(), e);
                // Không thay đổi trạng thái khi thất bại, để thử lại vào lần sau.
            }
        }
    }

    /**
     * Xử lý một giao dịch định kỳ đơn lẻ.
     * @param recurringTransfer Giao dịch cần xử lý.
     */
    private void processSingleRecurringTransfer(RecurringTransfer recurringTransfer) {
        // Cập nhật ngày thực thi tiếp theo
        updateNextExecutionDate(recurringTransfer);

        // Tăng số lần đã thực thi
        recurringTransfer.setExecutedOccurrences(recurringTransfer.getExecutedOccurrences() + 1);

        // Kiểm tra xem đã hoàn thành dựa trên số lần thực thi chưa
        if (recurringTransfer.getTotalOccurrences() != null &&
                recurringTransfer.getExecutedOccurrences() >= recurringTransfer.getTotalOccurrences()) {
            recurringTransfer.setStatus(RecurringTransferStatus.COMPLETED);
        }

        // Kiểm tra xem đã đến ngày kết thúc chưa
        if (recurringTransfer.getEndDate() != null &&
                recurringTransfer.getNextExecutionDate().isAfter(recurringTransfer.getEndDate())) {
            recurringTransfer.setStatus(RecurringTransferStatus.COMPLETED);
        }

        recurringTransferRepository.save(recurringTransfer);

        log.info("Recurring transfer processed: {}", recurringTransfer.getRecurringId());
    }

    /**
     * Cập nhật ngày thực thi tiếp theo dựa trên tần suất.
     * @param recurringTransfer Giao dịch định kỳ.
     */
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

    /**
     * Tạo ID duy nhất cho giao dịch định kỳ.
     * @return ID.
     */
    private String generateRecurringId() {
        return "REC_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * Chuyển đổi Entity sang DTO (hiện không được sử dụng, có thể xóa).
     */
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

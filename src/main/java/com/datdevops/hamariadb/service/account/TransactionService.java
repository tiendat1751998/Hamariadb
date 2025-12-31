package com.datdevops.hamariadb.service.account;


import com.datdevops.hamariadb.entity.Account;
import com.datdevops.hamariadb.entity.Transaction;
import com.datdevops.hamariadb.entity.TransactionStatus;
import com.datdevops.hamariadb.entity.TransactionType;
import com.datdevops.hamariadb.repository.dao.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service chịu trách nhiệm tạo và quản lý các bản ghi lịch sử giao dịch (Transaction).
 * Lớp này đóng vai trò là một lớp tiện ích cấp thấp hơn, được các service khác (như TransferService) sử dụng.
 */
@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Tạo một bản ghi giao dịch đã hoàn thành (COMPLETED).
     * Phương thức này giả định rằng việc thay đổi số dư đã được thực hiện ở nơi gọi.
     *
     * @param account        Tài khoản chính của giao dịch.
     * @param relatedAccount Tài khoản liên quan (ví dụ: tài khoản nhận tiền). Có thể null.
     * @param type           Loại giao dịch (DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN).
     * @param amount         Số tiền giao dịch (số âm cho giao dịch ghi nợ, số dương cho giao dịch ghi có).
     * @param description    Mô tả giao dịch.
     * @return Đối tượng Transaction đã được tạo và lưu.
     */
    public Transaction createTransaction(Account account, Account relatedAccount,
                                         TransactionType type, BigDecimal amount,
                                         String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionReference(generateTransactionReference());
        transaction.setAccount(account);
        transaction.setRelatedAccount(relatedAccount);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        // Số dư trước là số dư hiện tại của tài khoản.
        transaction.setBalanceBefore(account.getBalance());
        // Số dư sau được tính toán dựa trên số dư hiện tại và số tiền giao dịch.
        transaction.setBalanceAfter(account.getBalance().add(amount));
        transaction.setCurrency("VND");
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionDate(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    /**
     * Tạo một bản ghi giao dịch đang chờ xử lý (PENDING).
     * Thường được sử dụng cho các giao dịch cần các bước xử lý phức tạp hoặc xác nhận.
     *
     * @param account        Tài khoản chính.
     * @param relatedAccount Tài khoản liên quan.
     * @param type           Loại giao dịch.
     * @param amount         Số tiền.
     * @param description    Mô tả.
     * @return Đối tượng Transaction đã được tạo và lưu với trạng thái PENDING.
     */
    public Transaction createPendingTransaction(Account account, Account relatedAccount,
                                                TransactionType type, BigDecimal amount,
                                                String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionReference(generateTransactionReference());
        transaction.setAccount(account);
        transaction.setRelatedAccount(relatedAccount);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        // Số dư trước và sau đều là số dư hiện tại vì giao dịch chưa thực sự ảnh hưởng đến số dư.
        transaction.setBalanceBefore(account.getBalance());
        transaction.setBalanceAfter(account.getBalance());
        transaction.setCurrency("VND");
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    /**
     * Cập nhật trạng thái của một giao dịch từ PENDING sang COMPLETED.
     * @param transaction Giao dịch cần cập nhật.
     * @param account     Tài khoản liên quan để cập nhật số dư cuối cùng.
     */
    public void updateTransactionToCompleted(Transaction transaction, Account account) {
        transaction.setBalanceAfter(account.getBalance());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
    }

    /**
     * Cập nhật trạng thái của một giao dịch thành FAILED.
     * @param transaction Giao dịch cần cập nhật.
     * @param errorMessage Thông báo lỗi (nếu có).
     */
    public void updateTransactionToFailed(Transaction transaction, String errorMessage) {
        transaction.setStatus(TransactionStatus.FAILED);
        // TODO: Nên thêm một trường `errorMessage` vào entity Transaction để lưu lại lý do thất bại.
        // transaction.setErrorMessage(errorMessage);
        transactionRepository.save(transaction);
    }

    /**
     * Tạo một mã tham chiếu giao dịch duy nhất.
     * @return Chuỗi mã tham chiếu.
     */
    private String generateTransactionReference() {
        return "TX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

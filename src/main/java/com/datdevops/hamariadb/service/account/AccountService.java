package com.datdevops.hamariadb.service.account;


import com.datdevops.hamariadb.dto.response.AccountBalanceResponse;
import com.datdevops.hamariadb.dto.response.TransactionHistoryResponse;
import com.datdevops.hamariadb.entity.Account;
import com.datdevops.hamariadb.entity.Transaction;
import com.datdevops.hamariadb.entity.User;
import com.datdevops.hamariadb.repository.dao.AccountRepository;
import com.datdevops.hamariadb.repository.dao.TransactionRepository;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.repository.mapper.EntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service chịu trách nhiệm cho các hoạt động liên quan đến tài khoản người dùng,
 * như truy vấn số dư và lịch sử giao dịch.
 */
@Slf4j
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final EntityMapper entityMapper;

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          UserRepository userRepository, EntityMapper entityMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.entityMapper = entityMapper;
    }

    /**
     * Lấy thông tin số dư của một tài khoản cụ thể.
     * @param accountNumber Số tài khoản cần truy vấn.
     * @param username Tên người dùng thực hiện yêu cầu (để xác thực quyền).
     * @return Phản hồi chứa thông tin số dư.
     */
    public AccountBalanceResponse getAccountBalance(String accountNumber, String username) {
        // Tìm người dùng để xác thực.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tìm tài khoản và thông tin người dùng sở hữu.
        Account account = accountRepository.findByAccountNumberWithUser(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Kiểm tra xem người dùng có phải là chủ sở hữu của tài khoản không.
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this account");
        }

        // Sử dụng EntityMapper để chuyển đổi sang DTO.
        return entityMapper.toAccountBalanceResponse(account);
    }

    /**
     * Lấy danh sách tất cả các tài khoản đang hoạt động của một người dùng.
     * @param username Tên người dùng.
     * @return Danh sách các tài khoản và thông tin số dư.
     */
    public List<AccountBalanceResponse> getUserAccounts(String username) {
        List<Account> accounts = accountRepository.findActiveAccountsByUsername(username);

        // Chuyển đổi danh sách các entity Account thành danh sách DTO AccountBalanceResponse.
        return accounts.stream()
                .map(entityMapper::toAccountBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch sử giao dịch của một tài khoản với các bộ lọc và phân trang.
     * @param accountNumber Số tài khoản cần truy vấn.
     * @param username Tên người dùng để xác thực quyền.
     * @param fromDate Lọc giao dịch từ ngày (có thể null).
     * @param toDate Lọc giao dịch đến ngày (có thể null).
     * @param transactionType Lọc theo loại giao dịch (có thể null).
     * @param page Trang hiện tại (bắt đầu từ 0).
     * @param size Số lượng bản ghi trên mỗi trang.
     * @return Một trang (Page) chứa lịch sử giao dịch.
     */
    public Page<TransactionHistoryResponse> getTransactionHistory(String accountNumber, String username,
                                                                  LocalDateTime fromDate, LocalDateTime toDate,
                                                                  String transactionType, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = accountRepository.findByAccountNumberWithUser(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Xác thực quyền sở hữu tài khoản.
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this account");
        }

        // Tạo đối tượng Pageable để phân trang.
        Pageable pageable = PageRequest.of(page, size);
        // Gọi repository để truy vấn CSDL với các bộ lọc.
        Page<Transaction> transactions = transactionRepository.findByAccountAndFilters(
                accountNumber, fromDate, toDate, transactionType, pageable);

        // Chuyển đổi Page<Transaction> thành Page<TransactionHistoryResponse>.
        return transactions.map(entityMapper::toTransactionHistoryResponse);
    }
}

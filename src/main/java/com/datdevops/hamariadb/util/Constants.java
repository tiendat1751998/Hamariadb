package com.datdevops.hamariadb.util;


public class Constants {

    // Date formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_ZONE = "Asia/Ho_Chi_Minh";

    // Transaction types
    public static final String TRANSACTION_DEPOSIT = "DEPOSIT";
    public static final String TRANSACTION_WITHDRAWAL = "WITHDRAWAL";
    public static final String TRANSACTION_TRANSFER_OUT = "TRANSFER_OUT";
    public static final String TRANSACTION_TRANSFER_IN = "TRANSFER_IN";
    public static final String TRANSACTION_FEE = "FEE";
    public static final String TRANSACTION_INTEREST = "INTEREST";

    // Transfer status
    public static final String TRANSFER_PENDING = "PENDING";
    public static final String TRANSFER_PROCESSING = "PROCESSING";
    public static final String TRANSFER_COMPLETED = "COMPLETED";
    public static final String TRANSFER_FAILED = "FAILED";
    public static final String TRANSFER_CANCELLED = "CANCELLED";
    public static final String TRANSFER_REVERSED = "REVERSED";

    // Account status
    public static final String ACCOUNT_ACTIVE = "ACTIVE";
    public static final String ACCOUNT_INACTIVE = "INACTIVE";
    public static final String ACCOUNT_FROZEN = "FROZEN";
    public static final String ACCOUNT_CLOSED = "CLOSED";

    // User status
    public static final String USER_ACTIVE = "ACTIVE";
    public static final String USER_INACTIVE = "INACTIVE";
    public static final String USER_LOCKED = "LOCKED";

    // Roles
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_VIEW_BALANCE = "xem_so_du";
    public static final String ROLE_CREATE_TRANSACTION = "tao_giao_dich";
    public static final String ROLE_APPROVE_BATCH = "phe_duyet_giao_dich_lo";
    public static final String ROLE_MANAGE_USERS = "quan_ly_user";

    // Encryption
    public static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    public static final int AES_KEY_SIZE = 256;
    public static final int GCM_TAG_LENGTH = 128;
    public static final int IV_LENGTH = 12;

    // File processing
    public static final int BATCH_MAX_ROWS = 1000;
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] ALLOWED_FILE_TYPES = {"xlsx", "xls", "csv", "txt"};

    // Transfer limits
    public static final double MAX_SINGLE_TRANSFER = 500000000.0;
    public static final double DAILY_TRANSFER_LIMIT = 2000000000.0;

    private Constants() {
        // Utility class
    }
}

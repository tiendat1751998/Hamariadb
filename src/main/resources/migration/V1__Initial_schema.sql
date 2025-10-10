-- Create database
CREATE DATABASE IF NOT EXISTS core_banking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE core_banking;

-- Users table
CREATE TABLE users (
                       id VARCHAR(36) PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       phone_number VARCHAR(20),
                       full_name VARCHAR(100) NOT NULL,
                       status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') DEFAULT 'ACTIVE',
                       failed_login_attempts INT DEFAULT 0,
                       last_login_at TIMESTAMP NULL,
                       password_changed_at TIMESTAMP NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       INDEX idx_username (username),
                       INDEX idx_email (email),
                       INDEX idx_status (status)
);

-- Roles table
CREATE TABLE roles (
                       id VARCHAR(36) PRIMARY KEY,
                       role_code VARCHAR(50) UNIQUE NOT NULL,
                       role_name VARCHAR(100) NOT NULL,
                       description TEXT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       INDEX idx_role_code (role_code)
);

-- User roles table
CREATE TABLE user_roles (
                            id VARCHAR(36) PRIMARY KEY,
                            user_id VARCHAR(36) NOT NULL,
                            role_id VARCHAR(36) NOT NULL,
                            assigned_by VARCHAR(36),
                            assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            expires_at TIMESTAMP NULL,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                            FOREIGN KEY (assigned_by) REFERENCES users(id),
                            UNIQUE KEY unique_user_role (user_id, role_id),
                            INDEX idx_user_id (user_id),
                            INDEX idx_role_id (role_id)
);

-- Accounts table
CREATE TABLE accounts (
                          id VARCHAR(36) PRIMARY KEY,
                          account_number VARCHAR(20) UNIQUE NOT NULL,
                          user_id VARCHAR(36) NOT NULL,
                          account_type ENUM('SAVINGS', 'CURRENT', 'BUSINESS') DEFAULT 'SAVINGS',
                          balance DECIMAL(15,2) DEFAULT 0.00,
                          available_balance DECIMAL(15,2) DEFAULT 0.00,
                          currency VARCHAR(3) DEFAULT 'VND',
                          status ENUM('ACTIVE', 'INACTIVE', 'FROZEN', 'CLOSED') DEFAULT 'ACTIVE',
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                          INDEX idx_account_number (account_number),
                          INDEX idx_user_id (user_id),
                          INDEX idx_status (status)
);

-- Transactions table
CREATE TABLE transactions (
                              id VARCHAR(36) PRIMARY KEY,
                              transaction_reference VARCHAR(50) UNIQUE NOT NULL,
                              account_id VARCHAR(36) NOT NULL,
                              related_account_id VARCHAR(36),
                              transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_OUT', 'TRANSFER_IN', 'FEE', 'INTEREST') NOT NULL,
                              amount DECIMAL(15,2) NOT NULL,
                              balance_before DECIMAL(15,2) NOT NULL,
                              balance_after DECIMAL(15,2) NOT NULL,
                              currency VARCHAR(3) DEFAULT 'VND',
                              description TEXT,
                              status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',
                              transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (account_id) REFERENCES accounts(id),
                              FOREIGN KEY (related_account_id) REFERENCES accounts(id),
                              INDEX idx_transaction_ref (transaction_reference),
                              INDEX idx_account_id (account_id),
                              INDEX idx_transaction_date (transaction_date),
                              INDEX idx_type_status (transaction_type, status)
);

-- Transfers table
CREATE TABLE transfers (
                           id VARCHAR(36) PRIMARY KEY,
                           transfer_reference VARCHAR(50) UNIQUE NOT NULL,
                           from_account_id VARCHAR(36) NOT NULL,
                           to_account_number VARCHAR(20) NOT NULL,
                           to_bank_code VARCHAR(10),
                           to_bank_name VARCHAR(100),
                           to_account_name VARCHAR(100),
                           amount DECIMAL(15,2) NOT NULL,
                           fee DECIMAL(15,2) DEFAULT 0.00,
                           total_amount DECIMAL(15,2) NOT NULL,
                           currency VARCHAR(3) DEFAULT 'VND',
                           description TEXT,
                           transfer_type ENUM('INTERNAL', 'NAPAS', 'CITAD') DEFAULT 'INTERNAL',
                           status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REVERSED') DEFAULT 'PENDING',
                           error_message TEXT,
                           transaction_id VARCHAR(36),
                           initiated_by VARCHAR(36) NOT NULL,
                           approved_by VARCHAR(36),
                           completed_at TIMESTAMP NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           FOREIGN KEY (from_account_id) REFERENCES accounts(id),
                           FOREIGN KEY (transaction_id) REFERENCES transactions(id),
                           FOREIGN KEY (initiated_by) REFERENCES users(id),
                           FOREIGN KEY (approved_by) REFERENCES users(id),
                           INDEX idx_transfer_ref (transfer_reference),
                           INDEX idx_from_account (from_account_id),
                           INDEX idx_status (status),
                           INDEX idx_created_at (created_at)
);

-- Batch transfers table
CREATE TABLE batch_transfers (
                                 id VARCHAR(36) PRIMARY KEY,
                                 batch_id VARCHAR(50) UNIQUE NOT NULL,
                                 file_name VARCHAR(255),
                                 file_type VARCHAR(10),
                                 total_transactions INT DEFAULT 0,
                                 successful_transactions INT DEFAULT 0,
                                 failed_transactions INT DEFAULT 0,
                                 total_amount DECIMAL(15,2) DEFAULT 0.00,
                                 channel ENUM('CITAD', 'NAPAS') NOT NULL,
                                 status ENUM('UPLOADED', 'VALIDATING', 'VALIDATED', 'PROCESSING', 'COMPLETED', 'PARTIALLY_COMPLETED', 'FAILED') DEFAULT 'UPLOADED',
                                 validation_errors JSON,
                                 initiated_by VARCHAR(36) NOT NULL,
                                 approved_by VARCHAR(36),
                                 approved_at TIMESTAMP NULL,
                                 processed_at TIMESTAMP NULL,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 FOREIGN KEY (initiated_by) REFERENCES users(id),
                                 FOREIGN KEY (approved_by) REFERENCES users(id),
                                 INDEX idx_batch_id (batch_id),
                                 INDEX idx_status (status),
                                 INDEX idx_created_by (initiated_by)
);

-- Batch transfer items table
CREATE TABLE batch_transfer_items (
                                      id VARCHAR(36) PRIMARY KEY,
                                      batch_transfer_id VARCHAR(36) NOT NULL,
                                      sequence_number INT NOT NULL,
                                      to_account_number VARCHAR(20) NOT NULL,
                                      to_bank_code VARCHAR(10) NOT NULL,
                                      to_account_name VARCHAR(100),
                                      amount DECIMAL(15,2) NOT NULL,
                                      description TEXT,
                                      status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
                                      error_message TEXT,
                                      transfer_id VARCHAR(36),
                                      processed_at TIMESTAMP NULL,
                                      FOREIGN KEY (batch_transfer_id) REFERENCES batch_transfers(id) ON DELETE CASCADE,
                                      FOREIGN KEY (transfer_id) REFERENCES transfers(id),
                                      INDEX idx_batch_id (batch_transfer_id),
                                      INDEX idx_status (status)
);

-- Encryption keys table
CREATE TABLE encryption_keys (
                                 id VARCHAR(36) PRIMARY KEY,
                                 key_identifier VARCHAR(100) UNIQUE NOT NULL,
                                 user_id VARCHAR(36) NOT NULL,
                                 aes_key_encrypted TEXT NOT NULL,
                                 iv_encrypted TEXT NOT NULL,
                                 is_active BOOLEAN DEFAULT TRUE,
                                 expires_at TIMESTAMP NOT NULL,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                 INDEX idx_key_identifier (key_identifier),
                                 INDEX idx_user_id (user_id),
                                 INDEX idx_expires_at (expires_at)
);

-- Telegram notifications table
CREATE TABLE telegram_notifications (
                                        id VARCHAR(36) PRIMARY KEY,
                                        user_id VARCHAR(36) NOT NULL,
                                        chat_id VARCHAR(100) NOT NULL,
                                        message_type ENUM('BALANCE_UPDATE', 'TRANSACTION_SUCCESS', 'TRANSACTION_FAILED', 'SYSTEM_ALERT', 'BATCH_PROCESSED') NOT NULL,
                                        message_text TEXT NOT NULL,
                                        message_data JSON,
                                        status ENUM('SENT', 'FAILED', 'PENDING') DEFAULT 'PENDING',
                                        sent_at TIMESTAMP NULL,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (user_id) REFERENCES users(id),
                                        INDEX idx_user_id (user_id),
                                        INDEX idx_message_type (message_type),
                                        INDEX idx_created_at (created_at)
);

-- System alerts table
CREATE TABLE system_alerts (
                               id VARCHAR(36) PRIMARY KEY,
                               alert_type ENUM('SYSTEM_OVERLOAD', 'HIGH_LATENCY', 'ERROR_RATE', 'SECURITY_BREACH') NOT NULL,
                               severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
                               message TEXT NOT NULL,
                               metric_value DECIMAL(10,2),
                               threshold_value DECIMAL(10,2),
                               triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               resolved_at TIMESTAMP NULL,
                               is_resolved BOOLEAN DEFAULT FALSE,
                               INDEX idx_alert_type (alert_type),
                               INDEX idx_severity (severity),
                               INDEX idx_triggered_at (triggered_at)
);

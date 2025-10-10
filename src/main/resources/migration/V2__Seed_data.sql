USE core_banking;

-- Insert default roles
INSERT INTO roles (id, role_code, role_name, description) VALUES
                                                              (UUID(), 'xem_so_du', 'Xem số dư', 'Cho phép xem số dư tài khoản'),
                                                              (UUID(), 'tao_giao_dich', 'Tạo giao dịch', 'Cho phép tạo giao dịch chuyển tiền'),
                                                              (UUID(), 'phe_duyet_giao_dich_lo', 'Phê duyệt giao dịch lô', 'Cho phép phê duyệt giao dịch theo lô'),
                                                              (UUID(), 'quan_ly_user', 'Quản lý người dùng', 'Cho phép quản lý người dùng và phân quyền'),
                                                              (UUID(), 'admin', 'Quản trị viên', 'Toàn quyền quản trị hệ thống');

-- Insert admin user (password: admin123)
INSERT INTO users (id, username, password_hash, email, phone_number, full_name, status) VALUES
    (UUID(), 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVwUi.', 'admin@bank.com', '+84901234567', 'Quản trị viên hệ thống', 'ACTIVE');

-- Assign admin role to admin user
INSERT INTO user_roles (id, user_id, role_id, assigned_by)
SELECT
    UUID(),
    u.id,
    r.id,
    u.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_code = 'admin';

-- Insert sample customer user (password: customer123)
INSERT INTO users (id, username, password_hash, email, phone_number, full_name, status) VALUES
    (UUID(), 'customer001', '$2a$10$8R1yQ5Uc6o5Uc6o5Uc6o5u6o5Uc6o5Uc6o5Uc6o5Uc6o5Uc6o5Uc6o', 'customer001@email.com', '+84987654321', 'Nguyễn Văn Khách Hàng', 'ACTIVE');

-- Assign basic roles to customer
INSERT INTO user_roles (id, user_id, role_id, assigned_by)
SELECT
    UUID(),
    u.id,
    r.id,
    (SELECT id FROM users WHERE username = 'admin')
FROM users u, roles r
WHERE u.username = 'customer001' AND r.role_code IN ('xem_so_du', 'tao_giao_dich');

-- Insert sample accounts
INSERT INTO accounts (id, account_number, user_id, account_type, balance, available_balance, currency)
SELECT
    UUID(),
    '0123456789',
    id,
    'SAVINGS',
    100000000.00,
    100000000.00,
    'VND'
FROM users WHERE username = 'customer001';

INSERT INTO accounts (id, account_number, user_id, account_type, balance, available_balance, currency)
SELECT
    UUID(),
    '9876543210',
    id,
    'CURRENT',
    50000000.00,
    50000000.00,
    'VND'
FROM users WHERE username = 'admin';

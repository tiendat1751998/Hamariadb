package com.datdevops.hamariadb.service.admin;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datdevops.hamariadb.dto.request.UserCreateRequest;
import com.datdevops.hamariadb.dto.response.UserResponse;
import com.datdevops.hamariadb.entity.Account;
import com.datdevops.hamariadb.entity.Role;
import  com.datdevops.hamariadb.entity.User;
import com.datdevops.hamariadb.entity.UserRole;
import com.datdevops.hamariadb.entity.UserStatus;
import com.datdevops.hamariadb.repository.dao.RoleRepository;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.repository.mapper.EntityMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service dành cho quản trị viên để quản lý người dùng.
 */
@Slf4j
@Service
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper entityMapper;

    /**
     * Constructor để inject các dependency.
     */
    public UserManagementService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder, EntityMapper entityMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityMapper = entityMapper;
    }

    /**
     * Tạo một người dùng mới.
     * @param request Dữ liệu để tạo người dùng.
     * @param adminUsername Tên quản trị viên thực hiện.
     * @return Phản hồi chứa thông tin người dùng đã tạo.
     */
    public UserResponse createUser(UserCreateRequest request, String adminUsername) {
        // Kiểm tra xem username đã tồn tại chưa
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        // Kiểm tra xem email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
        Set<Account> accounts = new HashSet<>();
        Account account = new Account();
        // Tạo người dùng mới
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // Mã hóa mật khẩu
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setFullName(request.getFullName());
        user.setStatus(UserStatus.ACTIVE);
        account.setAccountNumber(request.getAccount().iterator().next());
        account.setUser(user);
        accounts.add(account);
        user.setAccounts(accounts);

        user = userRepository.save(user);

        // Gán vai trò nếu có
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            assignRolesToUser(user, request.getRoles(), adminUsername);
        }

        log.info("User created: {} by admin: {}", request.getUsername(), adminUsername);

        return entityMapper.toUserResponse(user);
    }

    /**
     * Lấy danh sách tất cả người dùng.
     * @param adminUsername Tên quản trị viên thực hiện (để ghi log).
     * @return Danh sách người dùng.
     */
    public List<UserResponse> getUsers(String adminUsername) {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(entityMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin chi tiết của một người dùng bằng ID.
     * @param userId ID của người dùng.
     * @param adminUsername Tên quản trị viên thực hiện.
     * @return Thông tin chi tiết người dùng.
     */
    public UserResponse getUserById(String userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return entityMapper.toUserResponse(user);
    }

    /**
     * Cập nhật vai trò cho một người dùng.
     * @param userId ID của người dùng.
     * @param roles Danh sách các mã vai trò mới.
     * @param adminUsername Tên quản trị viên thực hiện.
     * @return Thông tin người dùng sau khi cập nhật.
     */
    public UserResponse updateUserRoles(String userId, List<String> roles, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Xóa các vai trò hiện có
        user.getUserRoles().clear();
        userRepository.save(user);

        // Gán các vai trò mới
        if (roles != null && !roles.isEmpty()) {
            assignRolesToUser(user, roles, adminUsername);
        }

        log.info("User roles updated: {} by admin: {}", user.getUsername(), adminUsername);

        return entityMapper.toUserResponse(user);
    }

    /**
     * Cập nhật trạng thái của người dùng (ví dụ: ACTIVE, LOCKED).
     * @param userId ID của người dùng.
     * @param status Trạng thái mới.
     * @param adminUsername Tên quản trị viên thực hiện.
     * @return Thông tin người dùng sau khi cập nhật.
     */
    public UserResponse updateUserStatus(String userId, UserStatus status, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setStatus(status);
        user = userRepository.save(user);

        log.info("User status updated: {} to {} by admin: {}", user.getUsername(), status, adminUsername);

        return entityMapper.toUserResponse(user);
    }

    /**
     * Xóa một người dùng.
     * @param userId ID của người dùng.
     * @param adminUsername Tên quản trị viên thực hiện.
     */
    public void deleteUser(String userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Trong hệ thống thực tế, nên cân nhắc xóa mềm (soft delete) thay vì xóa cứng.
        userRepository.delete(user);

        log.info("User deleted: {} by admin: {}", user.getUsername(), adminUsername);
    }

    /**
     * Gán một danh sách vai trò cho người dùng.
     * @param user Đối tượng người dùng.
     * @param roleCodes Danh sách mã vai trò.
     * @param adminUsername Tên quản trị viên gán vai trò.
     */
    private void assignRolesToUser(User user, List<String> roleCodes, String adminUsername) {
        List<Role> roles = roleRepository.findByRoleCodeIn(roleCodes);

        // Đảm bảo tất cả các vai trò được yêu cầu đều tồn tại
        if (roles.size() != roleCodes.size()) {
            throw new RuntimeException("Some roles not found");
        }

        User adminUser = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRole.setAssignedBy(adminUser);
            user.getUserRoles().add(userRole);
        }

        userRepository.save(user);
    }

    /**
     * Chuyển đổi đối tượng User (Entity) sang UserResponse (DTO).
     * @param user Đối tượng Entity.
     * @return Đối tượng DTO.
     */
    private UserResponse convertToUserResponse(User user) {
        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleCode())
                .collect(Collectors.toList());

        return UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

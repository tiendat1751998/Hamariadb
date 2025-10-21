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

@Slf4j
@Service
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper entityMapper;

    public UserManagementService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder, EntityMapper entityMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityMapper = entityMapper;
    }

    public UserResponse createUser(UserCreateRequest request, String adminUsername) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
    Set<Account> accounts = new HashSet<>();
    Account account = new Account();
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setFullName(request.getFullName());
        user.setStatus(UserStatus.ACTIVE);
        account.setAccountNumber(request.getAccount());
        account.setUser(user);
        accounts.add(account);
        user.setAccounts(accounts);

        user = userRepository.save(user);

        // Assign roles if provided
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            assignRolesToUser(user, request.getRoles(), adminUsername);
        }

        log.info("User created: {} by admin: {}", request.getUsername(), adminUsername);

        return EntityMapper.toUserResponse(user);
    }

    public List<UserResponse> getUsers(String adminUsername) {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(String userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return EntityMapper.toUserResponse(user);
    }

    public UserResponse updateUserRoles(String userId, List<String> roles, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Clear existing roles
        user.getUserRoles().clear();
        userRepository.save(user);

        // Assign new roles
        if (roles != null && !roles.isEmpty()) {
            assignRolesToUser(user, roles, adminUsername);
        }

        log.info("User roles updated: {} by admin: {}", user.getUsername(), adminUsername);

        return EntityMapper.toUserResponse(user);
    }

    public UserResponse updateUserStatus(String userId, UserStatus status, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setStatus(status);
        user = userRepository.save(user);

        log.info("User status updated: {} to {} by admin: {}", user.getUsername(), status, adminUsername);

        return EntityMapper.toUserResponse(user);
    }

    public void deleteUser(String userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // In a real system, you might want to soft delete
        userRepository.delete(user);

        log.info("User deleted: {} by admin: {}", user.getUsername(), adminUsername);
    }

    private void assignRolesToUser(User user, List<String> roleCodes, String adminUsername) {
        List<Role> roles = roleRepository.findByRoleCodeIn(roleCodes);

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

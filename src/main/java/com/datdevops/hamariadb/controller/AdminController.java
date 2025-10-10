package com.datdevops.hamariadb.controller;


import com.datdevops.hamariadb.dto.request.UserCreateRequest;
import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.dto.response.UserResponse;
import com.datdevops.hamariadb.entity.Role;
import com.datdevops.hamariadb.entity.UserStatus;
import com.datdevops.hamariadb.service.admin.RoleService;
import com.datdevops.hamariadb.service.admin.UserManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final UserManagementService userManagementService;
    private final RoleService roleService;

    public AdminController(UserManagementService userManagementService, RoleService roleService) {
        this.userManagementService = userManagementService;
        this.roleService = roleService;
    }

    // User Management Endpoints
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @RequestBody UserCreateRequest request,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        log.info("Creating user by admin: {}", adminUsername);

        UserResponse response = userManagementService.createUser(request, adminUsername);
        return ResponseEntity.ok(ApiResponse.success(response, "User created successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(Authentication authentication) {
        String adminUsername = authentication.getName();
        log.info("Get users by admin: {}", adminUsername);

        List<UserResponse> responses = userManagementService.getUsers(adminUsername);
        return ResponseEntity.ok(ApiResponse.success(responses, "Users retrieved successfully"));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable String userId,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        log.info("Get user {} by admin: {}", userId, adminUsername);

        UserResponse response = userManagementService.getUserById(userId, adminUsername);
        return ResponseEntity.ok(ApiResponse.success(response, "User retrieved successfully"));
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable String userId,
            @RequestBody List<String> roles,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        log.info("Updating user roles for user: {} by admin: {}", userId, adminUsername);

        UserResponse response = userManagementService.updateUserRoles(userId, roles, adminUsername);
        return ResponseEntity.ok(ApiResponse.success(response, "User roles updated successfully"));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable String userId,
            @RequestParam UserStatus status,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        log.info("Updating user status for user: {} to {} by admin: {}", userId, status, adminUsername);

        UserResponse response = userManagementService.updateUserStatus(userId, status, adminUsername);
        return ResponseEntity.ok(ApiResponse.success(response, "User status updated successfully"));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String userId,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        log.info("Deleting user {} by admin: {}", userId, adminUsername);

        userManagementService.deleteUser(userId, adminUsername);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    // Role Management Endpoints
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<Role>>> getRoles(Authentication authentication) {
        log.info("Get roles by admin: {}", authentication.getName());

        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles, "Roles retrieved successfully"));
    }

    @GetMapping("/roles/{roleCode}")
    public ResponseEntity<ApiResponse<Role>> getRole(
            @PathVariable String roleCode,
            Authentication authentication) {

        log.info("Get role {} by admin: {}", roleCode, authentication.getName());

        Role role = roleService.getRoleByCode(roleCode);
        return ResponseEntity.ok(ApiResponse.success(role, "Role retrieved successfully"));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<Role>> createRole(
            @RequestParam String roleCode,
            @RequestParam String roleName,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        log.info("Creating role {} by admin: {}", roleCode, authentication.getName());

        Role role = roleService.createRole(roleCode, roleName, description);
        return ResponseEntity.ok(ApiResponse.success(role, "Role created successfully"));
    }

    @PutMapping("/roles/{roleCode}")
    public ResponseEntity<ApiResponse<Role>> updateRole(
            @PathVariable String roleCode,
            @RequestParam String roleName,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        log.info("Updating role {} by admin: {}", roleCode, authentication.getName());

        Role role = roleService.updateRole(roleCode, roleName, description);
        return ResponseEntity.ok(ApiResponse.success(role, "Role updated successfully"));
    }

    @DeleteMapping("/roles/{roleCode}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable String roleCode,
            Authentication authentication) {

        log.info("Deleting role {} by admin: {}", roleCode, authentication.getName());

        roleService.deleteRole(roleCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
    }
}

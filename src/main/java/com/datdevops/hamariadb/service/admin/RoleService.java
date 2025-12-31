package com.datdevops.hamariadb.service.admin;


import com.datdevops.hamariadb.entity.Role;
import com.datdevops.hamariadb.exception.BusinessException;
import com.datdevops.hamariadb.repository.dao.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service dành cho quản trị viên để quản lý các vai trò (Role) trong hệ thống.
 */
@Slf4j
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * Constructor để inject RoleRepository.
     */
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Lấy danh sách tất cả các vai trò.
     * @return Danh sách các đối tượng Role.
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Lấy một vai trò cụ thể bằng mã vai trò (roleCode).
     * @param roleCode Mã của vai trò cần tìm.
     * @return Đối tượng Role tương ứng.
     * @throws BusinessException nếu không tìm thấy vai trò.
     */
    public Role getRoleByCode(String roleCode) {
        return roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleCode));
    }

    /**
     * Tạo một vai trò mới.
     * @param roleCode Mã vai trò (duy nhất).
     * @param roleName Tên hiển thị của vai trò.
     * @param description Mô tả chi tiết về vai trò.
     * @return Đối tượng Role đã được tạo.
     * @throws BusinessException nếu mã vai trò đã tồn tại.
     */
    public Role createRole(String roleCode, String roleName, String description) {
        if (roleRepository.existsByRoleCode(roleCode)) {
            throw new BusinessException("Role already exists: " + roleCode);
        }

        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setDescription(description);

        role = roleRepository.save(role);

        log.info("Role created: {}", roleCode);

        return role;
    }

    /**
     * Cập nhật thông tin của một vai trò đã có.
     * @param roleCode Mã của vai trò cần cập nhật.
     * @param roleName Tên mới của vai trò.
     * @param description Mô tả mới của vai trò.
     * @return Đối tượng Role sau khi đã cập nhật.
     * @throws RuntimeException nếu không tìm thấy vai trò.
     */
    public Role updateRole(String roleCode, String roleName, String description) {
        Role role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

        role.setRoleName(roleName);
        role.setDescription(description);

        role = roleRepository.save(role);

        log.info("Role updated: {}", roleCode);

        return role;
    }

    /**
     * Xóa một vai trò khỏi hệ thống.
     * @param roleCode Mã của vai trò cần xóa.
     * @throws RuntimeException nếu không tìm thấy vai trò hoặc vai trò đang được gán cho người dùng.
     */
    public void deleteRole(String roleCode) {
        Role role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

        // Kiểm tra xem vai trò có đang được gán cho bất kỳ người dùng nào không.
        // Nếu có, không cho phép xóa để đảm bảo toàn vẹn dữ liệu.
        if (!role.getUserRoles().isEmpty()) {
            throw new RuntimeException("Cannot delete role that is assigned to users");
        }

        roleRepository.delete(role);

        log.info("Role deleted: {}", roleCode);
    }
}

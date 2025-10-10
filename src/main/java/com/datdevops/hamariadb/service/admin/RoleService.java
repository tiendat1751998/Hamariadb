package com.datdevops.hamariadb.service.admin;


import com.datdevops.hamariadb.entity.Role;
import com.datdevops.hamariadb.exception.BusinessException;
import com.datdevops.hamariadb.repository.dao.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRoleByCode(String roleCode) {
        return roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleCode));
    }

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

    public Role updateRole(String roleCode, String roleName, String description) {
        Role role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

        role.setRoleName(roleName);
        role.setDescription(description);

        role = roleRepository.save(role);

        log.info("Role updated: {}", roleCode);

        return role;
    }

    public void deleteRole(String roleCode) {
        Role role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

        // Check if role is assigned to any user
        if (!role.getUserRoles().isEmpty()) {
            throw new RuntimeException("Cannot delete role that is assigned to users");
        }

        roleRepository.delete(role);

        log.info("Role deleted: {}", roleCode);
    }
}

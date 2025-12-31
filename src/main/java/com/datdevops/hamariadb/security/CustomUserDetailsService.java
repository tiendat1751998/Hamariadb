package com.datdevops.hamariadb.security;


import com.datdevops.hamariadb.entity.User;
import com.datdevops.hamariadb.entity.UserStatus;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Service tùy chỉnh để tải thông tin chi tiết người dùng cho Spring Security.
 * Implement {@link UserDetailsService} để tích hợp với cơ chế xác thực của Spring.
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tải thông tin người dùng (bao gồm mật khẩu và vai trò) từ CSDL dựa trên username.
     * Spring Security sẽ sử dụng thông tin này để xác thực và phân quyền.
     *
     * @param username Tên đăng nhập của người dùng cần tải.
     * @return một đối tượng {@link UserDetails} chứa thông tin người dùng.
     * @throws UsernameNotFoundException nếu không tìm thấy người dùng với username tương ứng.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm người dùng trong CSDL, kèm theo thông tin vai trò (roles) để tối ưu hóa query
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Kiểm tra các trạng thái của tài khoản
        boolean enabled = user.getStatus() == UserStatus.ACTIVE; // Tài khoản có được kích hoạt không?
        boolean accountNonExpired = true; // Tài khoản có hết hạn không? (luôn là true trong trường hợp này)
        boolean credentialsNonExpired = true; // Mật khẩu có hết hạn không? (luôn là true)
        boolean accountNonLocked = user.getStatus() != UserStatus.LOCKED; // Tài khoản có bị khóa không?

        // Chuyển đổi danh sách vai trò từ entity UserRole sang các đối tượng SimpleGrantedAuthority
        // Spring Security yêu cầu vai trò phải có tiền tố "ROLE_"
        var authorities = user.getUserRoles().stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getRoleCode()))
                .collect(Collectors.toList());

        // Trả về một đối tượng User của Spring Security, chứa các thông tin cần thiết cho việc xác thực
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                authorities
        );
    }
}

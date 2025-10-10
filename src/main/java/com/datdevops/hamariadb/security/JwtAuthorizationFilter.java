//package com.datdevops.hamariadb.security;
//
//
//import com.datdevops.hamariadb.config.JwtTokenUtil;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class JwtAuthorizationFilter extends OncePerRequestFilter {
//
//    private final JwtTokenUtil jwtTokenUtil;
//
//    public JwtAuthorizationFilter(JwtTokenUtil jwtTokenUtil) {
//        this.jwtTokenUtil = jwtTokenUtil;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        String token = getJwtFromRequest(request);
//
//        if (token != null && jwtTokenUtil.validateToken(token)) {
//            String username = jwtTokenUtil.getUsernameFromToken(token);
//            List<String> roles = jwtTokenUtil.getRolesFromToken(token);
//
//            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                List<SimpleGrantedAuthority> authorities = roles.stream()
//                        .map(SimpleGrantedAuthority::new)
//                        .collect(Collectors.toList());
//
//                UsernamePasswordAuthenticationToken authentication =
//                        new UsernamePasswordAuthenticationToken(username, null, authorities);
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    private String getJwtFromRequest(HttpServletRequest request) {
//        String bearerToken = request.getHeader("Authorization");
//        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7);
//        }
//        return null;
//    }
//}

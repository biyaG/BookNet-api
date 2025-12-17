package it.unipi.booknetapi.shared.lib.security;

import it.unipi.booknetapi.shared.lib.authentication.JwtService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // 1. Check for "Bearer <token>"
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // 2. Verify token
                UserToken userToken = jwtService.validateToken(token);

                // 3. Tell Spring Security who the user is
                // We map "ADMIN" -> "ROLE_ADMIN" for Spring's hasRole() checks
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userToken.getRole().name());

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userToken, null, Collections.singletonList(authority)
                );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                // Token invalid: ensure context is clear
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

}
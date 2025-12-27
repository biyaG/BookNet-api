package it.unipi.booknetapi.shared.lib.security;

import it.unipi.booknetapi.shared.lib.authentication.JwtService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
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
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        // System.out.println("Token: " + token);

        // 1. Check for "Bearer <token>"
        if (token != null && token.startsWith("Bearer ")) {

            try {
                // 2. Verify token
                UserToken userToken = jwtService.validateToken(token.replace("Bearer ", ""));
                // System.out.println("User: " + userToken.getIdUser());

                // 3. Tell Spring Security who the user is
                // We map "ADMIN" -> "ROLE_ADMIN" for Spring's hasRole() checks
                // SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userToken.getRole().name());
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(userToken.getRole().name());

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userToken, null, Collections.singletonList(authority)
                );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                // Token invalid: ensure context is clear
                // System.out.println("Invalid token: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

}
package it.unipi.booknetapi.shared.lib.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless APIs
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // No Sessions
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()

                        /*
                        .requestMatchers("/api/auth/register/**").permitAll() // Registering endpoints are public
                        .requestMatchers("/api/auth/login").permitAll() // Login endpoint is public

                        .requestMatchers("/api/user/**").permitAll() // Private endpoint

                        .requestMatchers("/api/book/public/**").permitAll() // Public endpoint
                        .requestMatchers("/api/author/public/**").permitAll() // Public endpoint

                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // Only for role ADMIN

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        .requestMatchers("/actuator/**").permitAll()

                        .anyRequest().authenticated() // All others require valid token
                        */
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // Add our filter
                .build();
    }

}
package com.pcr.dataserver;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    @Bean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService admins(ServerSettings settings) {
        if (settings.adminPassword() == null || settings.adminPassword().length() < 12)
            throw new IllegalStateException("Set DATASERVER_ADMIN_PASSWORD to at least 12 characters");
        return new InMemoryUserDetailsManager(User.withUsername("admin")
            .password(new BCryptPasswordEncoder().encode(settings.adminPassword())).roles("ADMIN").build());
    }

    @Bean @Order(1) SecurityFilterChain devices(HttpSecurity http, DeviceService devices) throws Exception {
        http.securityMatcher("/api/device/**")
            .csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().hasRole("DEVICE"))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> res.sendError(401)));
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                String id = request.getHeader("X-Device-Id");
                if (!devices.authenticate(id, request.getHeader("X-Device-Key"))) {
                    response.setStatus(401); response.setContentType("application/json");
                    response.getWriter().write("{\"Result\":\"0\",\"Message\":\"Invalid device credentials\"}"); return;
                }
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))));
                SecurityContextHolder.setContext(context);
                try { chain.doFilter(request, response); } finally { SecurityContextHolder.clearContext(); }
            }
        }, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean @Order(2) SecurityFilterChain admin(HttpSecurity http, ServerSettings settings) throws Exception {
        return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/upload").access((authentication,context) ->
                    new org.springframework.security.authorization.AuthorizationDecision(settings.legacyUploadEnabled()))
                .requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().denyAll())
            .httpBasic(b -> {}).build();
    }
}

package com.medafrica.mavex.security.filter;



import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.UserRepository;
import com.medafrica.mavex.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Pas de header ou ne commence pas par "Bearer " → on passe
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String email = jwtService.extractEmail(jwt);

            // Déjà authentifié → on passe
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null && user.isActive() && jwtService.isTokenValid(jwt, user)) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Token malformé → on laisse passer sans authentifier
        }

        filterChain.doFilter(request, response);
    }

    @Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    // Routes publiques — le filtre JWT ne s'applique pas
    // /api/auth/me/permissions est intentionnellement exclu : il nécessite un JWT
    return path.equals("/login")
        || path.equals("/api/auth/login")
        || path.equals("/api/auth/refresh")
        || path.equals("/api/auth/logout")
        || path.equals("/api/auth/forgot-password")
        || path.equals("/api/auth/reset-password")
        || path.startsWith("/css/")
        || path.startsWith("/js/")
        || path.startsWith("/images/");
}
}

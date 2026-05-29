package com.medafrica.mavex.security.config;

import com.medafrica.mavex.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            // JWT = stateless → aucune session HTTP
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ── Pages Thymeleaf : toutes publiques
                // La protection est gérée côté JS (vérif localStorage)
                .requestMatchers(
                    "/",
                    "/login",
                    "/dashboard",
                    "/shipments/**",
                    "/orders/**",
                    "/clients/**",
                    "/shippers/**",
                    "/imports/**",
                    "/users/**",
                    "/error",
                    "/favicon.ico"
                ).permitAll()

                // ── Ressources statiques : publiques
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/fonts/**",
                    "/webjars/**"
                ).permitAll()

                // ── API Auth : publique (login, refresh, forgot, reset)
                .requestMatchers("/api/auth/**").permitAll()

                 // ── Paiement : publique (accès via lien email sans JWT)
                // /api/payments/pay/{token}/initiate  → le client initie le paiement
                // /api/payments/success               → retour PayPal après approbation
                // /api/payments/cancel                → retour PayPal après annulation
                .requestMatchers("/api/payments/**").permitAll()

                // ── Swagger : public
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // ── Toutes les autres API : JWT obligatoire
                .anyRequest().authenticated()
            )

            // Pas de form login Spring → c'est le JS fetch qui gère
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // JWT filter avant le filtre d'authentification standard
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
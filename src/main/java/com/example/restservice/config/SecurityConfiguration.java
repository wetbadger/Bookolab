package com.example.restservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    private final AuthenticationProvider authenticationProvider;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfiguration(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/gs-guide-websocket/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()

                        // 1. SPECIFIC FIRST: Explicitly require auth for the edit sub-path
                        .requestMatchers("/api/pages/*/edit").authenticated()

                        // 2. GENERAL SECOND: Allow public GET viewing for standard pages
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/pages/*").permitAll()

                        // 3. FALLBACK: Everything else stays completely locked down
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration allowPagesConfig = new CorsConfiguration();
        allowPagesConfig.setAllowedOrigins(List.of(allowedOrigin)); // e.g., http://localhost:5173
        allowPagesConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS")); // Added OPTIONS just in case of preflight checks
        allowPagesConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        allowPagesConfig.setAllowCredentials(true);

        CorsConfiguration denyEditConfig = new CorsConfiguration();
        denyEditConfig.setAllowedOrigins(List.of());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 1. Apply your standard CORS rules to the API pages
        source.registerCorsConfiguration("/api/pages/*", allowPagesConfig);
        source.registerCorsConfiguration("/api/pages/*/edit", denyEditConfig);

        // Highlight-start: 2. Apply your allowed origins to your auth endpoints!
        source.registerCorsConfiguration("/auth/**", allowPagesConfig);
        // Highlight-end

        return source;
    }
}

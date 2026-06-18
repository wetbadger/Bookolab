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
import org.springframework.web.cors.reactive.CorsUtils;

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
                        // 1. CRITICAL: Authenticated exceptions go first
                        .requestMatchers("/api/pages/*/edit").authenticated()
                        .requestMatchers("/api/authors/me").authenticated() // Fix: Fully match the prefix used by Axios

                        // 2. Broad permitAll mappings go second
                        .requestMatchers("/gs-guide-websocket/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/words/**").permitAll()
                        .requestMatchers("/api/authors/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/pages/*").permitAll()

                        // 3. Fallback
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
        allowPagesConfig.setAllowedOrigins(List.of(allowedOrigin));
        // Best practice: Allow PUT and DELETE alongside GET/POST/OPTIONS for standard REST operations
        allowPagesConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        allowPagesConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        allowPagesConfig.setAllowCredentials(true);

        CorsConfiguration denyEditConfig = new CorsConfiguration();
        denyEditConfig.setAllowedOrigins(List.of());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Notice the double asterisks (/**) to match nested resources correctly
        source.registerCorsConfiguration("/api/pages/**", allowPagesConfig);
        source.registerCorsConfiguration("/api/pages/*/edit", denyEditConfig);
        source.registerCorsConfiguration("/**", allowPagesConfig); // This will now catch /auth/login and /auth/signup

        return source;
    }
}

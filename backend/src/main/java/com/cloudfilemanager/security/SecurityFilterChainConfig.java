package com.cloudfilemanager.security;

import com.cloudfilemanager.ratelimit.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityFilterChainConfig {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final RateLimitFilter rateLimitFilter;

    public SecurityFilterChainConfig(
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            DelegatedAuthEntryPoint authenticationEntryPoint,
            RateLimitFilter rateLimitFilter) {
        this.authenticationProvider = authenticationProvider;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * RateLimitFilter is a @Component so Spring manages its config/lifecycle, but it must
     * run inside the Security filter chain (after JWT auth resolves the principal) rather
     * than as a separately auto-registered servlet filter - otherwise it would run twice
     * (once globally, once via addFilterAfter below) or see an unresolved SecurityContext.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> disableAutoRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/resend-verification"
                        )
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/ping",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/verify-email/**",
                                "/actuator/**",
                                "/api/v1/share/**"
                        )
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/verify-email")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        rateLimitFilter,
                        JwtAuthenticationFilter.class
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                );
        return http.build();
    }
}

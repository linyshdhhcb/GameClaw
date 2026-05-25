package ai.gameclaw.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "gameclaw.security.mode", havingValue = "sso", matchIfMissing = false)
    public SecurityFilterChain ssoFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/onboarding/**", "/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "gameclaw.security.mode", havingValue = "dev", matchIfMissing = true)
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .build();
    }
}

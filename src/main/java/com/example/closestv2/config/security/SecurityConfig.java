package com.example.closestv2.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 시큐리티 설정
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    /**
     * 시큐리티 무시 옵션 설정
     * - WebSecurityCustomizer
     *
     * @return
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {  //아래의 filterChain 에서도 관리 가능
        return web -> web.ignoring()
                .requestMatchers("/favicon")
                .requestMatchers("/error")
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**"));
    }

    /**
     * 경로에 대한 권한 허용 설정, 로그인 설정
     * - SecurityFilterChain
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(HttpBasicConfigurer::disable) // -> rest api
                .csrf(CsrfConfigurer::disable) // -> rest api
                .sessionManagement(configurer -> configurer
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))  //jwt는 세션을 갖지 않음
                .authorizeHttpRequests((request) ->
                        request
                                .anyRequest().permitAll()) // --> 이슈해결 이것 없으면 PreAuthorize 적용 안됐음
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

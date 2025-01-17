package com.example.closestv2.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.example.closestv2.config.security.TokenConstants.*;
import static com.example.closestv2.config.security.TokenConstants.TokenType.ACCESS_TOKEN;
import static com.example.closestv2.config.security.TokenConstants.TokenType.REFRESH_TOKEN;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    // 요청 -> filter -> servlet -> interceptor -> aop -> controller
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<Token> accessTokenOptional = resolveTokenByCookie(request, ACCESS_TOKEN_COOKIE.getValue());
        ;
        Optional<Token> refreshTokenOptional = resolveTokenByCookie(request, REFRESH_TOKEN_COOKIE.getValue());
        if (accessTokenOptional.isEmpty() && refreshTokenOptional.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
//        Token accessToken = accessTokenOptional.get() //todo accessToken만 없는 경우 refresh 처리하기
        boolean isAccessTokenValidate;
        if (accessTokenOptional.isEmpty() || !tokenProvider.validateToken(accessTokenOptional.get(), ACCESS_TOKEN)) {
            isAccessTokenValidate = false;
        } else {
            isAccessTokenValidate = true;
        }

        boolean isRefreshTokenValidate;
        if (refreshTokenOptional.isEmpty() || !tokenProvider.validateToken(refreshTokenOptional.get(), REFRESH_TOKEN)) {
            isRefreshTokenValidate = false;
        } else {
            isRefreshTokenValidate = true;
        }

        if (isAccessTokenValidate) {
            // 엑세스 토큰 유효
            authenticate(accessTokenOptional.get());
        } else if (isRefreshTokenValidate) {
            // 리프레시 토큰 유효
            Token newAccessToken = tokenProvider.issueToken(refreshTokenOptional.get());
            authenticate(newAccessToken);
            addAccessTokenToCookie(response, newAccessToken); //todo refreshToken renewal?
        } else {
            throw new IOException();
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(Token accessToken) {
        String userEmail = tokenProvider.getSubject(accessToken, TokenType.ACCESS_TOKEN);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, "", List.of());
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken); // 시큐리티 컨텍스트에 인증 정보 담기
    }

    // 헤더에서 엑세스 토큰 얻기
    private Optional<Token> resolveAccessTokenByHeader(HttpServletRequest request, String headerKey) {
        String headerValue = request.getHeader(headerKey);
        if (StringUtils.isBlank(headerValue) || !headerValue.startsWith(TOKEN_PREFIX.getValue())) {
            return Optional.empty();
        }
        return Optional.of(tokenProvider.resolveToken(headerValue, TOKEN_PREFIX.getValue()));
    }

    // 쿠키에서 리프레시 토큰 얻기
    private Optional<Token> resolveTokenByCookie(HttpServletRequest request, String cookieKey) {
        Cookie refreshTokenCookie = WebUtils.getCookie(request, cookieKey);
        if (refreshTokenCookie == null) {
            return Optional.empty();
        }
        String cookieValue = refreshTokenCookie.getValue();
        Token refreshToken = tokenProvider.resolveToken(cookieValue);
        return Optional.of(refreshToken);
    }

    private void addAccessTokenToCookie(HttpServletResponse response, Token accessToken) {
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken.getTokenValue());
        accessTokenCookie.setHttpOnly(true); // JavaScript에서 쿠키에 접근 불가능하도록 설정
        accessTokenCookie.setMaxAge(60 * 30); // 쿠키 유효 기간 설정
        response.addCookie(accessTokenCookie);
    }

    private void addRefreshTokenToCookie(HttpServletResponse response, Token refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken.getTokenValue());
        refreshTokenCookie.setHttpOnly(true); // JavaScript에서 쿠키에 접근 불가능하도록 설정
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 30); // 쿠키 유효 기간 설정 (예: 30일)
        response.addCookie(refreshTokenCookie);
    }
}

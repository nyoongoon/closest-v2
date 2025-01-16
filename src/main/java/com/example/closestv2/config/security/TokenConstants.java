package com.example.closestv2.config.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenConstants {
    TOKEN_HEADER("Authorization"),
    TOKEN_PREFIX ("Bearer "),
    KEY_ROLES("roles"),
    TOKEN_COOKIE("refreshToken");

    private final String value;

    public enum TokenType {
        ACCESS_TOKEN,
        REFRESH_TOKEN
    }
}

package com.example.closestv2.infrastructure.jwt;

import com.example.closestv2.config.security.Token;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JwtToken implements Token {
    private final String tokenValue;
}

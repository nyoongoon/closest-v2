package com.example.closestv2.config.security;

import java.util.List;

public interface TokenProvider {
    Token resolveToken(String value);
    Token resolveToken(String value, String tokenPrefix);

    Token issueToken(Token refreshToken);

    Token issueToken(TokenConstants.TokenType tokenTypes, String userEmail, List<Authority> roles);

    String getSubject(Token token, TokenConstants.TokenType tokenType);

    boolean validateToken(Token token, TokenConstants.TokenType tokenType);
}

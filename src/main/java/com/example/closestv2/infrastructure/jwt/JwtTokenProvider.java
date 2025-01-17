package com.example.closestv2.infrastructure.jwt;

import com.example.closestv2.config.security.Authority;
import com.example.closestv2.config.security.Token;
import com.example.closestv2.config.security.TokenConstants.TokenType;
import com.example.closestv2.config.security.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.SERVER_ERROR;
import static com.example.closestv2.config.security.TokenConstants.TokenType.ACCESS_TOKEN;
import static com.example.closestv2.config.security.TokenConstants.TokenType.REFRESH_TOKEN;

/**
 * 토큰 구현 클래스 - Jwt 토큰
 */
@Component
public class JwtTokenProvider implements TokenProvider {
    private static final long ACCESS_TOKEN_EXPIRED_TIME = 1000 * 60 * 30; //밀리세컨드*초*분* == 30분
    private static final long REFRESH_TOKEN_EXPIRED_TIME = 1000 * 60 * 60 * 24; //밀리세컨드*초*분*시 == 24시간

    @Value("{spring.jwt.access-secret-key}")
    private String accessSecretKey;

    @Value("{spring.jwt.refresh-secret-key}")
    private String refreshSecretKey;

    private final String KEY_ROLES = "roles";


    /**
     * 토큰 생성
     *
     * @param claims
     * @param secretKey
     * @param expiredTime
     * @return
     */
    private Token generateToken(Claims claims, String secretKey, long expiredTime) {
        Date now = new Date();
        //jwt Token
         String tokenValue = Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + expiredTime)) // 토큰 만료 시간
                .signWith(SignatureAlgorithm.HS256, secretKey)  // 시그니처 알고리즘, 비밀키
                .compact();
        return new JwtToken(tokenValue);
    }

    // 토큰 유효성 체크
    public String getSubject(Token token, TokenType tokenType) {
        // 토큰 만료 경우 예외 발생
        String secretKey = ACCESS_TOKEN == tokenType ? accessSecretKey : refreshSecretKey;
        return parseClaims(token, secretKey).getSubject();
    }

    private Claims parseClaims(Token token, String secretKey) {
        // 토큰 만료 경우 예외 발생
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token.getTokenValue()).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // 토큰 발급
    public Token issueToken(Token refreshToken) {
        // 사용자의 권한정보를 저장하기 위한 클레임 생성
        Claims claims = this.parseClaims(refreshToken, refreshSecretKey);
        // 엑세스 토큰 재발급
        return this.generateToken(claims, accessSecretKey, ACCESS_TOKEN_EXPIRED_TIME);
    }

    public Token issueToken(TokenType tokenTypes, String userEmail, List<Authority> roles) {
        // 사용자의 권한정보를 저장하기 위한 클레임 생성
        Claims claims = Jwts.claims().setSubject(userEmail);
        claims.put(KEY_ROLES, roles); // 클레임은 키밸류
        //Access Token & Refresh Token
        Token token;
        if(tokenTypes == ACCESS_TOKEN){
            token = generateToken(claims, this.accessSecretKey, ACCESS_TOKEN_EXPIRED_TIME);
        }else if(tokenTypes == REFRESH_TOKEN){
            token = generateToken(claims, this.refreshSecretKey, REFRESH_TOKEN_EXPIRED_TIME);
        }else{
            throw new IllegalStateException(SERVER_ERROR);
        }

        return token;
    }

    @Override
    public Token resolveToken(String value) {
        return new JwtToken(value);
    }

    @Override
    public Token resolveToken(String value, String tokenPrefix) {
        String tokenValue = value.substring(tokenPrefix.length());
        return new JwtToken(tokenValue);
    }
//    public String getRefreshTokenByClaims(Claims claims) {
//        return this.generateToken(claims, this.refreshSecretKey, REFRESH_TOKEN_EXPIRED_TIME);
//    }
//
//

  
    public boolean validateToken(Token token, TokenType tokenType) {
        if (!StringUtils.hasText(token.getTokenValue())) {
            return false;
        }
        String secretKey = tokenType == ACCESS_TOKEN ? accessSecretKey : refreshSecretKey;
        Claims claims = parseClaims(token, secretKey);
        return !claims.getExpiration().before(new Date());
    }


//    public boolean validateAccessToken(Token token) {
//        return validateToken(token, this.accessSecretKey);
//    }
//
//
//    public boolean validateRefreshToken(Token token) {
//        return validateToken(token.getTokenValue(), this.refreshSecretKey);
//    }





}

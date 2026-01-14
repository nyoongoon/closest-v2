package com.example.closestv2.api.controller;

import com.example.closestv2.api.MemberAuthApi;
import com.example.closestv2.api.service.model.request.MemberAuthSigninPostServiceRequest;
import com.example.closestv2.api.service.model.request.MemberAuthSignupPostServiceRequest;
import com.example.closestv2.api.usecases.MemberAuthUsecase;
import com.example.closestv2.config.security.Token;
import com.example.closestv2.config.security.TokenConstants.TokenType;
import com.example.closestv2.models.MemberAuthSigninPostRequest;
import com.example.closestv2.models.MemberAuthSignupPostRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MemberAuthController implements MemberAuthApi {
    private final MemberAuthUsecase memberAuthUsecase;

    @Override
    public ResponseEntity<Void> memberAuthSignupPost(MemberAuthSignupPostRequest request) {
        memberAuthUsecase.signUp(toServiceRequest(request));
        return ResponseEntity.ok().build();
    }

    private MemberAuthSignupPostServiceRequest toServiceRequest(MemberAuthSignupPostRequest request) {
        return new MemberAuthSignupPostServiceRequest(request.getEmail(), request.getPassword(),
                request.getConfirmPassword());
    }

    @Override
    public ResponseEntity<Void> memberAuthSigninPost(MemberAuthSigninPostRequest request) {
        Map<TokenType, Token> tokens = memberAuthUsecase.signIn(toServiceRequest(request));
        Token accessToken = tokens.get(TokenType.ACCESS_TOKEN);
        Token refreshToken = tokens.get(TokenType.REFRESH_TOKEN);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken.getTokenValue())
                .httpOnly(false)
                .sameSite("Strict")
                .maxAge(60 * 30)
                .path("/")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken.getTokenValue())
                .httpOnly(true)
                .sameSite("Strict")
                .maxAge(60 * 60 * 24 * 30)
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .build();
    }

    private MemberAuthSigninPostServiceRequest toServiceRequest(MemberAuthSigninPostRequest request) {
        return new MemberAuthSigninPostServiceRequest(request.getEmail(), request.getPassword());
    }
}

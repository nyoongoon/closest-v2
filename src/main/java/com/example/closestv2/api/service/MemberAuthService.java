package com.example.closestv2.api.service;

import com.example.closestv2.api.service.model.request.MemberAuthSigninPostServiceRequest;
import com.example.closestv2.api.service.model.request.MemberAuthSignupPostServiceRequest;
import com.example.closestv2.api.usecases.MemberAuthUsecase;
import com.example.closestv2.config.security.Token;
import com.example.closestv2.config.security.TokenConstants;
import com.example.closestv2.config.security.TokenProvider;
import com.example.closestv2.domain.member.MemberRepository;
import com.example.closestv2.domain.member.MemberRoot;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.*;
import static com.example.closestv2.config.security.Authority.ROLE_USER;
import static com.example.closestv2.config.security.TokenConstants.*;
import static com.example.closestv2.config.security.TokenConstants.TokenType.ACCESS_TOKEN;
import static com.example.closestv2.config.security.TokenConstants.TokenType.REFRESH_TOKEN;

@Service
@Validated
@RequiredArgsConstructor
public class MemberAuthService implements MemberAuthUsecase {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Override
    public void signUp(@Valid MemberAuthSignupPostServiceRequest serviceRequest) {
        String email = serviceRequest.getEmail();
        String password = serviceRequest.getPassword();
        String confirmPassword = serviceRequest.getConfirmPassword();

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException(NOT_EQUAL_PASSWORDS);
        }

        boolean isDuplicated = memberRepository.existsByMemberInfoUserEmail(email);
        if (isDuplicated) {
            throw new IllegalArgumentException(DUPLICATED_EMAIL);
        }

        MemberRoot memberRoot = MemberRoot.create(serviceRequest.getEmail(), serviceRequest.getPassword()); //todo passwordEncoder
        memberRepository.save(memberRoot);
    }

    @Override
    public Map<TokenType, Token> signIn(MemberAuthSigninPostServiceRequest serviceRequest) {
        //todo passwordencorder
        String email = serviceRequest.getEmail();
        String password = serviceRequest.getPassword();
        MemberRoot memberRoot = memberRepository.findByMemberInfoUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(INVALID_MEMBER));
        if(!password.equals(memberRoot.getMemberInfo().getPassword())){
            throw new IllegalArgumentException(INVALID_MEMBER);
        }
        Token accessToken = tokenProvider.issueToken(ACCESS_TOKEN, email, List.of(ROLE_USER));
        Token refreshToken = tokenProvider.issueToken(REFRESH_TOKEN, email, List.of(ROLE_USER));
        Map<TokenType, Token> tokens = Map.of(
                ACCESS_TOKEN, accessToken,
                REFRESH_TOKEN, refreshToken
        );
        return tokens;
    }
}

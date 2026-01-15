package com.example.closestv2.api.usecases;

import com.example.closestv2.api.service.model.request.MemberAuthSigninPostServiceRequest;
import com.example.closestv2.api.service.model.request.MemberAuthSignupPostServiceRequest;
import com.example.closestv2.config.security.Token;
import com.example.closestv2.config.security.TokenConstants;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.example.closestv2.config.security.TokenConstants.*;

@Service
public interface MemberAuthUsecase {
    void signUp(@Valid MemberAuthSignupPostServiceRequest serviceRequest);
    Map<TokenType, Token> signIn(MemberAuthSigninPostServiceRequest serviceRequest);
}

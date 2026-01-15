package com.example.closestv2.api.service;

import com.example.closestv2.api.service.model.request.MemberAuthSigninPostServiceRequest;
import com.example.closestv2.api.service.model.request.MemberAuthSignupPostServiceRequest;
import com.example.closestv2.config.security.Token;
import com.example.closestv2.config.security.TokenProvider;
import com.example.closestv2.domain.member.MemberRepository;
import com.example.closestv2.domain.member.MemberRoot;
import com.example.closestv2.infrastructure.jwt.JwtToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static com.example.closestv2.config.security.Authority.ROLE_USER;
import static com.example.closestv2.config.security.TokenConstants.TokenType.ACCESS_TOKEN;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MemberAuthServiceTest {
    private final String VALID_EMAIL = "abc@google.com";
    private final String VALID_PASSWORD = "Abc1234!!";
    private final String NOT_VALID_PASSWORD = "Abc1234";
    private final String NOT_SAME_PASSWORD = "Cba1234!!";
    private final String VALID_CONFIRM_PASSWORD = "Abc1234!!";
    private final String NOT_SAME_CONFIRM_PASSWORD = "Cba1234!!";

    private MemberAuthService sut;

    @Mock
    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(tokenProvider.issueToken(any(), any(), any())).thenReturn(new JwtToken("value"));
        sut = new MemberAuthService(memberRepository, passwordEncoder, tokenProvider);
    }

    @Test
    @DisplayName("이메일, 비밀번호, 확인 비밀번호로 회원가입을 진행한다.")
    void signUp() {
        //given
        when(memberRepository.existsByMemberInfoUserEmail(VALID_EMAIL)).thenReturn(false);
        when(memberRepository.save(any())).thenReturn(any());
        //expected
        assertThatCode(() -> sut.signUp(new MemberAuthSignupPostServiceRequest(VALID_EMAIL, VALID_PASSWORD, VALID_CONFIRM_PASSWORD)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비밀번호, 확인 비밀번호가 다르면 에러가 발생한다.")
    void signUpNotEqualsPasswords() {
        assertThatThrownBy(() -> sut.signUp(new MemberAuthSignupPostServiceRequest(VALID_EMAIL, VALID_PASSWORD, NOT_SAME_CONFIRM_PASSWORD)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("비밀번호, 확인 비밀번호는 8글자 이상의 대소문자와 특수문자로 이뤄지지 않으면 에러가 발생한다.")
    void signUpNotValidPatternPasswords() {
        assertThatThrownBy(() -> sut.signUp(new MemberAuthSignupPostServiceRequest(VALID_EMAIL, NOT_VALID_PASSWORD, VALID_CONFIRM_PASSWORD))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sut.signUp(new MemberAuthSignupPostServiceRequest(VALID_EMAIL, VALID_PASSWORD, NOT_SAME_CONFIRM_PASSWORD))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이미 존재하는 이메일일 경우 에러가 발생한다.")
    void signUpDuplicatedEmail() {
        //given
        when(memberRepository.existsByMemberInfoUserEmail(VALID_EMAIL)).thenReturn(true);
        //expected
        assertThatThrownBy(() -> sut.signUp(new MemberAuthSignupPostServiceRequest(VALID_EMAIL, VALID_PASSWORD, VALID_CONFIRM_PASSWORD)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이메일, 비밀번호로 로그인한다.")
    void signIn() {
        //given
        MemberRoot memberRoot = MemberRoot.create(VALID_EMAIL, VALID_PASSWORD, "별명");
        when(memberRepository.findByMemberInfoUserEmail(VALID_EMAIL)).thenReturn(Optional.of(memberRoot));
        //when
        assertThatCode(()->sut.signIn(new MemberAuthSigninPostServiceRequest(VALID_EMAIL, VALID_PASSWORD)))
                .doesNotThrowAnyException();
    }
    @Test
    @DisplayName("가입된 비밀번호와 다른 비밀번호로 로그인 시 에러가 발생한다.")
    void signInNotSamePassword() {
        //given
        MemberRoot memberRoot = MemberRoot.create(VALID_EMAIL, VALID_PASSWORD, "별명");
        when(memberRepository.findByMemberInfoUserEmail(VALID_EMAIL)).thenReturn(Optional.of(memberRoot));
        //when
        assertThatThrownBy(() -> sut.signIn(new MemberAuthSigninPostServiceRequest(VALID_EMAIL, NOT_SAME_PASSWORD)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("가입된 Email이 존재하지 않으면 에러가 발생한다.")
    void signInNotFoundEmail() {
        //when
        assertThatThrownBy(() -> sut.signIn(new MemberAuthSigninPostServiceRequest(VALID_EMAIL, NOT_SAME_PASSWORD)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
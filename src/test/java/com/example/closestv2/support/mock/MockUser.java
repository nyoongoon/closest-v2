package com.example.closestv2.support.mock;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = MockSecurityContext.class)
public @interface MockUser {
    String nickName() default "닉네임";

    String email() default "mock@naver.com";

    String password() default "Abc1234!";
}

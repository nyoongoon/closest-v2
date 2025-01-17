package com.example.closestv2.support.mock;

import com.example.closestv2.domain.member.MemberRoot;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

/**
 * 테스트용 스프링 시큐리티 컨텍스트에 Authentication 추가
 */
public class MockSecurityContext implements WithSecurityContextFactory<MockUser> {

    @Override
    public SecurityContext createSecurityContext(MockUser annotation) {

        MemberRoot memberRoot = MemberRoot.create(
                annotation.email(),
                annotation.password(),
                annotation.nickName()
        );

        var userRole = new SimpleGrantedAuthority("ROLE_USER");
        var auth = new UsernamePasswordAuthenticationToken(
                memberRoot,
                null,
                List.of(userRole)
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}

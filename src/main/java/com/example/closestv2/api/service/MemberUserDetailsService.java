package com.example.closestv2.api.service;

import com.example.closestv2.domain.member.MemberInfo;
import com.example.closestv2.domain.member.MemberRepository;
import com.example.closestv2.domain.member.MemberRoot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.MEMBER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberRoot memberRoot = memberRepository.findByMemberInfoUserEmail(username)
                .orElseThrow(() -> new IllegalArgumentException(MEMBER_NOT_FOUND));
        MemberInfo memberInfo = memberRoot.getMemberInfo();
        String userEmail = memberInfo.getUserEmail();
        String password = memberInfo.getPassword();

        return new User(userEmail, password, List.of());
    }
}

package com.example.closestv2.api.service;

import com.example.closestv2.domain.member.MemberRepository;
import com.example.closestv2.domain.member.MemberRoot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MyBlogSaveService {
    private final MemberRepository memberRepository;

    @Transactional
    public void saveMyBlog(String memberEmail, URL blogUrl){
        MemberRoot memberRoot = memberRepository.findByMemberInfoUserEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException(MEMBER_NOT_FOUND));

        memberRoot.saveMyBlog(blogUrl, 0);
    }
}

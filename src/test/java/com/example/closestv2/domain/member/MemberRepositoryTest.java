package com.example.closestv2.domain.member;

import com.example.closestv2.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

class MemberRepositoryTest extends RepositoryTestSupport {
    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("UserEmail로 MemberRoot를 조회한다.")
    void findByMemberInfoUserEmail(){
        //given
        MemberRoot memberRoot = MemberRoot.create("abc@naver.com", "Abc1234!!", "별명");
        memberRepository.save(memberRoot);
        //when
        MemberRoot found = memberRepository.findByMemberInfoUserEmail("abc@naver.com").orElseThrow();
        //then
        assertThat(found).isEqualTo(memberRoot);
    }

    @Test
    @DisplayName("UserEmail로 MemberRoot를 존재여부 판단한다.")
    void existsByMemberInfoUserEmail(){
        boolean isExists1 = memberRepository.existsByMemberInfoUserEmail("abc@naver.com");
        assertThat(isExists1).isEqualTo(false);
        //given
        MemberRoot memberRoot = MemberRoot.create("abc@naver.com", "Abc1234!!", "별명");
        memberRepository.save(memberRoot);
        //when
        boolean isExists2 = memberRepository.existsByMemberInfoUserEmail("abc@naver.com");
        //then
        assertThat(isExists2).isEqualTo(true);
    }
}
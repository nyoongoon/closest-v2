package com.example.closestv2.domain.member;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRoot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Valid
    @Embedded
    private MemberInfo memberInfo;

    @Embedded
    private MyBlog myBlog;

    @Builder(access = AccessLevel.PRIVATE)
    private MemberRoot(
            String userEmail,
            String password,
            String nickName
    ) {
        this.memberInfo = MemberInfo.builder()
                .userEmail(userEmail)
                .password(password)
                .nickName(nickName)
                .build();
    }

    public static MemberRoot create(
            String userEmail,
            String password,
            String nickName
    ) {
        return MemberRoot.builder()
                .userEmail(userEmail)
                .password(password)
                .nickName(nickName)
                .build();
    }

    public boolean hasMyBlog() {
        if (ObjectUtils.isEmpty(myBlog)) {
            return false;
        }
        URL url = myBlog.url();
        if (ObjectUtils.isEmpty(url)) { //url이 존재하면 나의 블로그 존재
            return false;
        }
        return true;
    }

    public void createMyBlog(

    ){

    }
}
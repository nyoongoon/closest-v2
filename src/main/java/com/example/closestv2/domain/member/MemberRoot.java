package com.example.closestv2.domain.member;

import com.example.closestv2.domain.member.event.StatusMessageEditEvent;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.ALREADY_EXISTS_MY_BLOG;
import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_EXISTS_MY_BLOG;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRoot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Valid
    @Embedded
    private MemberInfo memberInfo;

    @Valid
    @Embedded
    private MyBlog myBlog;

    @Builder(access = AccessLevel.PRIVATE)
    private MemberRoot(
            MemberInfo memberInfo
    ) {
        this.memberInfo = memberInfo;
    }

    public static MemberRoot create(
            String userEmail,
            String password
    ) {
       return create(userEmail, password, null);
    }

    public static MemberRoot create(
            String userEmail,
            String password,
            String nickName
    ) {
        MemberInfo memberInfo = MemberInfo.builder()
                .userEmail(userEmail)
                .password(password)
                .nickName(nickName)
                .build();
        return MemberRoot.builder()
                .memberInfo(memberInfo)
                .build();
    }

    public boolean hasMyBlog() {
        if (ObjectUtils.isEmpty(myBlog)) {
            return false;
        }
        URL blogUrl = myBlog.getBlogUrl();
        if (ObjectUtils.isEmpty(blogUrl)) { //url이 존재하면 나의 블로그 존재
            return false;
        }
        return true;
    }

    public void saveMyBlog(
            URL blogUrl,
            long myBlogVisitCount
    ) {
        if (hasMyBlog()) {
            throw new IllegalStateException(ALREADY_EXISTS_MY_BLOG); //블로그 변경 시 변경 메서드 사용
        }

        myBlog = MyBlog.builder()
                .blogUrl(blogUrl)
                .myBlogVisitCount(myBlogVisitCount)
                .build();
    }

    public void visitMyBlog() {
        if (!hasMyBlog()) {
            throw new IllegalStateException(ALREADY_EXISTS_MY_BLOG); //블로그 변경 시 변경 메서드 사용
        }
        long plusedMyBlogVisitCount = myBlog.getMyBlogVisitCount() + 1;
        myBlog = MyBlog.builder()
                .blogUrl(myBlog.getBlogUrl())
                .myBlogVisitCount(plusedMyBlogVisitCount)
                .statusMessage(myBlog.getStatusMessage())
                .build();
    }

    // 이벤트 발생
    public StatusMessageEditEvent withStatusMessage(String statusMessage) {
        if (!hasMyBlog()) {
            throw new IllegalStateException(NOT_EXISTS_MY_BLOG);
        }
        URL blogUrl = myBlog.getBlogUrl();
        long myBlogVisitCount = myBlog.getMyBlogVisitCount();
        myBlog = MyBlog.builder()
                .blogUrl(blogUrl)
                .statusMessage(statusMessage)
                .myBlogVisitCount(myBlogVisitCount)
                .build();

        return new StatusMessageEditEvent(blogUrl, statusMessage);
    }
}

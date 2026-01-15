package com.example.closestv2.domain.blog;

import com.example.closestv2.util.constant.SpecificDate;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.proxy.HibernateProxy;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.*;

@Slf4j
@Getter
@Entity
@Table(name = "blog")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlogRoot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id")
    private Long id;

    @Valid
    @Embedded
    private BlogInfo blogInfo;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "postUrl") // 엔티티인 경우
    private Map<URL, Post> posts = new HashMap<>();

    @Builder(access = AccessLevel.PRIVATE)
    private BlogRoot(
            BlogInfo blogInfo) {
        this.blogInfo = blogInfo;
    }

    public static BlogRoot create(
            URL rssUrl,
            URL blogUrl,
            String blogTitle,
            String author,
            URL thumbnailUrl) {
        LocalDateTime epochTime = SpecificDate.EPOCH_TIME.getLocalDateTime();
        return create(
                rssUrl,
                blogUrl,
                blogTitle,
                author,
                thumbnailUrl,
                epochTime);
    }

    public static BlogRoot create(
            URL rssUrl,
            URL blogUrl,
            String blogTitle,
            String author,
            URL thumbnailUrl,
            LocalDateTime publishedDateTime) {
        BlogInfo blogInfo = BlogInfo.builder()
                .rssUrl(rssUrl)
                .blogUrl(blogUrl)
                .blogTitle(blogTitle)
                .author(author)
                .thumbnailUrl(thumbnailUrl)
                .publishedDateTime(publishedDateTime)
                .blogVisitCount(0L)
                .build();
        return BlogRoot.builder()
                .blogInfo(blogInfo)
                .build();
    }

    public Post createPost(
            URL postUrl,
            String postTitle,
            LocalDateTime publishedDateTime) {
        Post post = Post.builder()
                .postUrl(postUrl)
                .postTitle(postTitle)
                .publishedDateTime(publishedDateTime)
                .postVisitCount(0L)
                .build();
        return post;
    }

    public void editStatusMessage(String statusMessage) {
        blogInfo = BlogInfo.builder()
                .rssUrl(blogInfo.getRssUrl())
                .blogUrl(blogInfo.getBlogUrl())
                .blogTitle(blogInfo.getBlogTitle())
                .author(blogInfo.getAuthor())
                .publishedDateTime(blogInfo.getPublishedDateTime())
                .blogVisitCount(blogInfo.getBlogVisitCount())
                .statusMessage(statusMessage)
                .build();
    }

    public boolean isBlogUpdated( // todo 업데이트 여부.. 일단 추가하지 않고 성능 이슈되면 추가하기??
            BlogRoot comparedBlogRoot) {
        BlogInfo comparedBlogInfo = comparedBlogRoot.blogInfo;

        if (!blogInfo.getBlogTitle().equals(comparedBlogInfo.getBlogTitle())) {
            return true;
        }
        if (!blogInfo.getAuthor().equals(comparedBlogInfo.getAuthor())) {
            return true;
        }
        if (blogInfo.getPublishedDateTime().isBefore(comparedBlogInfo.getPublishedDateTime())) {
            return true;
        }

        return false;
    }

    public void updateBlogRoot(
            BlogRoot comparedBlogRoot) {
        BlogInfo comparedBlogInfo = comparedBlogRoot.blogInfo;
        checkValidUpdate(comparedBlogInfo);

        blogInfo = BlogInfo.builder()
                .rssUrl(blogInfo.getRssUrl())
                .blogUrl(blogInfo.getBlogUrl())
                .blogTitle(comparedBlogInfo.getBlogTitle())
                .author(comparedBlogInfo.getAuthor())
                .publishedDateTime(comparedBlogInfo.getPublishedDateTime())
                .blogVisitCount(blogInfo.getBlogVisitCount())
                .statusMessage(blogInfo.getStatusMessage())
                .build();

        updatePosts(comparedBlogRoot);
    }

    /**
     * //todo 업데이트 여부.. 일단 추가하지 않고 성능 이슈되면 추가하기??
     * 블로그 발행시간을 비교하므로, 메서드 호출 시 미리 변경할 대상의 블로그 발행시간을 업데이트하면 안된다.
     * Post 중 가장 발생시간이 최신인 것
     */
    private boolean isPostsUpdated(
            BlogRoot comparedBlogRoot) {
        BlogInfo comparedBlogInfo = comparedBlogRoot.blogInfo;
        checkValidUpdate(comparedBlogInfo);

        if (blogInfo.getPublishedDateTime().isBefore(comparedBlogInfo.getPublishedDateTime())) {
            return true;
        }

        return false;
    }

    private void updatePosts(
            BlogRoot comparedBlogRoot) {
        checkValidUpdate(comparedBlogRoot.blogInfo);
        LocalDateTime recentPublishedDateTime = blogInfo.getPublishedDateTime();

        Map<URL, Post> comparedPosts = comparedBlogRoot.getPosts();
        for (Map.Entry<URL, Post> urlPostEntry : comparedPosts.entrySet()) {
            Post comparedPost = urlPostEntry.getValue();
            LocalDateTime updatePostPublishedDateTime = comparedPost.getPublishedDateTime();
            if (recentPublishedDateTime.isBefore(updatePostPublishedDateTime)) {
                recentPublishedDateTime = updatePostPublishedDateTime;
            }
            if (!posts.containsKey(comparedPost.getPostUrl())) {
                log.info("post inserted : {}", comparedPost.getPostUrl());
                posts.put(comparedPost.getPostUrl(), comparedPost);
                continue;
            }

            Post originPost = posts.get(comparedPost.getPostUrl());
            if (originPost.isUpdated(comparedPost)) { // posts에 postUrl 있는 경우 제목이나 발행시간이 다르면 업데이트
                log.info("post updated : {}", comparedPost.getPostUrl());
                originPost.updateTitle(comparedPost.getPostTitle());
                originPost.updatePublishedDateTime(comparedPost.getPublishedDateTime());
            }
        }
        updatePublishedDateTime(recentPublishedDateTime); // blogInfo publishedDateTime 최신화
    }

    public void updatePublishedDateTime(LocalDateTime recentPublishedDateTime) {
        blogInfo = BlogInfo.builder()
                .rssUrl(blogInfo.getRssUrl())
                .blogUrl(blogInfo.getBlogUrl())
                .blogTitle(blogInfo.getBlogTitle())
                .author(blogInfo.getAuthor())
                .publishedDateTime(recentPublishedDateTime)
                .blogVisitCount(blogInfo.getBlogVisitCount())
                .statusMessage(blogInfo.getStatusMessage())
                .build();
    }

    private void checkValidUpdate(BlogInfo comparedBlogInfo) {
        if (!blogInfo.getRssUrl().equals(comparedBlogInfo.getRssUrl())) {
            log.error("RSS URL이 다른 블로그 정보로 업데이트 시도 - 기존: {}, 업데이트 시도: {}", blogInfo.getBlogUrl(),
                    comparedBlogInfo.getBlogUrl());
            throw new IllegalStateException(BLOG_UPDATABLE_BY_SAME_RSS_URL);
        }
        // blogInfo의 발행시간이 더 이후면 예외
        if (blogInfo.getPublishedDateTime().isAfter(comparedBlogInfo.getPublishedDateTime())) {
            log.error("블로그 업데이트는 발행시간이 더 과거인 블로그로 업데이트 할 수 없다. 기존:{}, 업데이트 시도:{} ", blogInfo.getPublishedDateTime(),
                    comparedBlogInfo.getPublishedDateTime());
            throw new IllegalStateException(BLOG_NON_UPDATABLE_BY_PAST_PUBLISHED_DATETIME);
        }
    }

    public void visit() {
        blogInfo = BlogInfo.builder()
                .rssUrl(blogInfo.getRssUrl())
                .blogUrl(blogInfo.getBlogUrl())
                .blogTitle(blogInfo.getBlogTitle())
                .author(blogInfo.getAuthor())
                .publishedDateTime(blogInfo.getPublishedDateTime())
                .blogVisitCount(blogInfo.getBlogVisitCount() + 1)
                .statusMessage(blogInfo.getStatusMessage())
                .build();
    }

    public void visitPost(URL postUrl) {
        if (!posts.containsKey(postUrl)) {
            throw new IllegalArgumentException(NOT_EXISTS_POST_URL);
        }

        Post post = posts.get(postUrl);
        long postVisitCount = post.getPostVisitCount();

        Post updatedPost = Post.builder()
                .postUrl(post.getPostUrl())
                .postTitle(post.getPostTitle())
                .publishedDateTime(post.getPublishedDateTime())
                .postVisitCount(postVisitCount + 1)
                .build();
        posts.replace(post.getPostUrl(), updatedPost);
    }

    @Override
    public final boolean equals(Object o) { // todo 다른 엔티티에도 적용
        if (this == o)
            return true;
        if (o == null)
            return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        BlogRoot blogRoot = (BlogRoot) o;
        return getId() != null && Objects.equals(getId(), blogRoot.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}

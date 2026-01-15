package com.example.closestv2.domain.blog;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.net.URL;
import java.time.LocalDateTime;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @NotNull(message = POST_URL_IS_REQUIRED)
    @Column(name = "post_url")
    private URL postUrl;

    @NotBlank(message = POST_TITLE_IS_REQUIRED)
    private String postTitle;

    @NotNull(message = POST_PUBLISHED_DATETIME_IS_REQUIRED)
    private LocalDateTime publishedDateTime;

    @NotNull(message = POST_VISIT_COUNT_IS_REQUIRED)
    private long postVisitCount;

    @Builder(access = AccessLevel.PROTECTED)
    private Post(
            URL postUrl,
            String postTitle,
            LocalDateTime publishedDateTime,
            long postVisitCount) {
        Assert.notNull(postUrl, POST_URL_IS_REQUIRED);
        Assert.hasText(postTitle, POST_TITLE_IS_REQUIRED);
        Assert.notNull(publishedDateTime, POST_PUBLISHED_DATETIME_IS_REQUIRED);

        this.postUrl = postUrl;
        this.postTitle = postTitle;
        this.publishedDateTime = publishedDateTime;
        this.postVisitCount = postVisitCount;
    }

    public boolean isUpdated(Post compared) {
        if (!postTitle.equals(compared.postTitle)) {
            return true;
        }
        if (!publishedDateTime.equals(compared.publishedDateTime)) {
            return true;
        }

        return false;
    }

    public void updateTitle(String updateTitle) {
        postTitle = updateTitle;
    }

    public void updatePublishedDateTime(LocalDateTime updatePublishedDateTime) {
        publishedDateTime = updatePublishedDateTime;
    }

    // @Override
    // public final boolean equals(Object o) {
    // if (this == o) return true;
    // if (o == null) return false;
    // Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy)
    // o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    // Class<?> thisEffectiveClass = this instanceof HibernateProxy ?
    // ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() :
    // this.getClass();
    // if (thisEffectiveClass != oEffectiveClass) return false;
    // Post post = (Post) o;
    // return getId() != null && Objects.equals(getId(), post.getId());
    // }
    //
    // @Override
    // public final int hashCode() {
    // return this instanceof HibernateProxy ? ((HibernateProxy)
    // this).getHibernateLazyInitializer().getPersistentClass().hashCode() :
    // getClass().hashCode();
    // }
}

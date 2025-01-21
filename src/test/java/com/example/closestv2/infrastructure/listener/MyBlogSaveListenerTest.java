package com.example.closestv2.infrastructure.listener;

import com.example.closestv2.api.service.MyBlogSaveService;
import com.example.closestv2.domain.blog.event.MyBlogSaveEvent;
import com.example.closestv2.util.url.UrlUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MyBlogSaveListenerTest {
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final URL ANY_BLOG_URI = UrlUtils.from(URI.create("http://www.example.com"));

    @Mock
    private MyBlogSaveService myBlogSaveService;

    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Spring 컨텍스트 설정
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(MyBlogSaveListener.class, () -> new MyBlogSaveListener(myBlogSaveService));
        context.refresh();
        eventPublisher = context;
    }

    @Test
    @DisplayName("MyBlog 저장 이벤트가 발행되면 해당 이벤트를 수신하고 응용 서비스를 호출한다.")
    void onMyBlogSaveEvent() {
        //given
        MyBlogSaveEvent event = new MyBlogSaveEvent(ANY_MEMBER_EMAIL, ANY_BLOG_URI);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        //when
        eventPublisher.publishEvent(event);
        // then
        verify(myBlogSaveService, times(1)).saveMyBlog(emailCaptor.capture(), urlCaptor.capture());
        assertThat(emailCaptor.getValue()).isEqualTo(ANY_MEMBER_EMAIL);
        assertThat(urlCaptor.getValue()).isEqualTo(ANY_BLOG_URI);
    }
}
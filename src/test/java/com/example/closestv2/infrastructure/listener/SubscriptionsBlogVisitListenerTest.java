package com.example.closestv2.infrastructure.listener;

import com.example.closestv2.api.service.BlogVisitService;
import com.example.closestv2.domain.subscription.event.SubscriptionsBlogVisitEvent;
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

class SubscriptionsBlogVisitListenerTest {
    private final long ANY_SUBSCRIPTION_ID = 1L;
    private final URL ANY_BLOG_URL = UrlUtils.from(URI.create("http://www.example.com"));

    @Mock
    private BlogVisitService blogVisitService;

    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Spring 컨텍스트 설정
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(SubscriptionsBlogVisitListener.class, () -> new SubscriptionsBlogVisitListener(blogVisitService));
        context.refresh();
        eventPublisher = context;
    }

    @Test
    @DisplayName("SubscriptionsBlogVisit 이벤트 발생 시 이벤트를 수신하고 BlogVisitService#visitBlog()를 호출한다.")
    void onSubscriptionsBlogVisitListen(){
        //given
        SubscriptionsBlogVisitEvent event = new SubscriptionsBlogVisitEvent(ANY_SUBSCRIPTION_ID, ANY_BLOG_URL);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        //when
        eventPublisher.publishEvent(event);
        //then
        verify(blogVisitService, times(1)).visitBlog(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo(ANY_BLOG_URL);
    }
}
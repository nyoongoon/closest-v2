package com.example.closestv2.infrastructure.listener;

import com.example.closestv2.api.service.BlogEditService;
import com.example.closestv2.domain.member.event.StatusMessageEditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


class StatusMessageChangeEditListenerTest {
    private final URL ANY_BLOG_URL = URI.create("http://example.com").toURL();
    private final String ANY_STATUS_MESSAGE = "변경된 상태 메시지";

    @Mock
    private BlogEditService blogEditService;

    private ApplicationEventPublisher eventPublisher;

    StatusMessageChangeEditListenerTest() throws MalformedURLException {
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Spring 컨텍스트 설정
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(BlogEditService.class, () -> blogEditService); // Mock 주입
        context.registerBean(StatusMessageChangeEditListener.class, () -> new StatusMessageChangeEditListener(blogEditService));
        context.refresh();
        eventPublisher = context;
    }

    @Test
    @DisplayName("블로그 상태메시지 수정 이벤트가 발행되면 해당 이벤트를 수신하고 관련 응용서비스를 호출한다.")
    void onStatusMessageEditEvent(){
        //given
        StatusMessageEditEvent event = new StatusMessageEditEvent(ANY_BLOG_URL, ANY_STATUS_MESSAGE);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        //when
        eventPublisher.publishEvent(event);
        // then
        verify(blogEditService, times(1)).editStatueMessage(urlCaptor.capture(), messageCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo(ANY_BLOG_URL);
        assertThat(messageCaptor.getValue()).isEqualTo(ANY_STATUS_MESSAGE);
    }
}
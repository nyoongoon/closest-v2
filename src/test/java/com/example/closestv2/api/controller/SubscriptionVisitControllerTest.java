package com.example.closestv2.api.controller;

import com.example.closestv2.api.service.model.response.VisitSubscriptionResponse;
import com.example.closestv2.support.ControllerTestSupport;
import com.example.closestv2.support.mock.MockUser;
import com.example.closestv2.util.url.UrlUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.net.URL;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



class SubscriptionVisitControllerTest extends ControllerTestSupport {
    private final long ANY_SUBSCRIPTION_ID = 1L;
    private final URL ANY_URL = UrlUtils.from(URI.create("http://example.com/"));

    @MockUser
    @Test
    @DisplayName("구독id를 요청하여 해당 블로그 uri로 301 코드 리다이렉트 응답 받는다.")
    void subscriptionsSubscriptionsIdVisitGet() throws Exception {
        //given
        VisitSubscriptionResponse response = new VisitSubscriptionResponse(ANY_URL);
        when(subscriptionVisitUsecase.visitSubscription(ANY_SUBSCRIPTION_ID)).thenReturn(response);
        //when
        //then
        mockMvc.perform(MockMvcRequestBuilders.get("/subscriptions/{subscriptionsId}/visit", ANY_SUBSCRIPTION_ID))
                .andDo(print())
                .andExpect(status().isMovedPermanently()) // HTTP 302 상태 코드 검증
                .andExpect(header().string("Location", ANY_URL.toURI().toString())) // 리다이렉트 URL 검증
        ;
    }
}
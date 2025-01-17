package com.example.closestv2.domain.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionInfoTest {
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final long ANY_SUBSCRIPTION_VISIT_COUNT = 0L;

    private SubscriptionInfo sut;
    private SubscriptionInfo.SubscriptionInfoBuilder builder;

    @BeforeEach
    void setUp() {
        builder = SubscriptionInfo.builder()
                .memberEmail(ANY_MEMBER_EMAIL)
                .subscriptionVisitCount(ANY_SUBSCRIPTION_VISIT_COUNT);
    }

    @Test
    @DisplayName("SubscriptionInfo 생성 성공 케이스")
    void createSubscriptionInfoSuccessTest() {
        sut = builder.build();
        assertThat(sut.getMemberEmail()).isEqualTo(ANY_MEMBER_EMAIL);
        assertThat(sut.getSubscriptionVisitCount()).isEqualTo(ANY_SUBSCRIPTION_VISIT_COUNT);
    }
}
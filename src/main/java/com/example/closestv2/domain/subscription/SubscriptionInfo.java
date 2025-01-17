package com.example.closestv2.domain.subscription;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.util.Assert;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.MEMBER_ID_IS_REQUIRED;
import static com.example.closestv2.api.exception.ExceptionMessageConstants.SUBSCRIPTION_VISIT_COUNT_IS_REQUIRED;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class SubscriptionInfo {
    @NotNull(message = MEMBER_ID_IS_REQUIRED)
    private String memberEmail;

    @NotNull(message = SUBSCRIPTION_VISIT_COUNT_IS_REQUIRED)
    private long subscriptionVisitCount;

    private String subscriptionNickName;

    @Builder(access = AccessLevel.PROTECTED)
    private SubscriptionInfo(
            String memberEmail,
            long subscriptionVisitCount,
            String subscriptionNickName
    ) {
        this.memberEmail = memberEmail;
        this.subscriptionVisitCount = subscriptionVisitCount;
        this.subscriptionNickName = subscriptionNickName;
    }
}

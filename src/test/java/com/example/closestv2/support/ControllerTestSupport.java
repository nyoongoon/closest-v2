package com.example.closestv2.support;

import com.example.closestv2.api.controller.SubscriptionVisitController;
import com.example.closestv2.api.usecases.SubscriptionVisitUsecase;
import com.example.closestv2.config.security.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers =
        SubscriptionVisitController.class
)
public class ControllerTestSupport {
    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;

//    @Autowired
//    protected ObjectMapper objectMapper;

    @MockBean
    protected SubscriptionVisitUsecase subscriptionVisitUsecase;
}

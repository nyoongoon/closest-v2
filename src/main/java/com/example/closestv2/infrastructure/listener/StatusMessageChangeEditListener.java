package com.example.closestv2.infrastructure.listener;

import com.example.closestv2.api.service.BlogEditService;
import com.example.closestv2.domain.member.event.StatusMessageEditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
@RequiredArgsConstructor
public class StatusMessageChangeEditListener {
    private final BlogEditService blogEditService;

    @EventListener
    public void onStatusMessageEditEvent(StatusMessageEditEvent event) {
        URL blogUrl = event.blogUrl();
        String statusMessage = event.statusMessage();
        blogEditService.editStatueMessage(blogUrl, statusMessage);
    }
}

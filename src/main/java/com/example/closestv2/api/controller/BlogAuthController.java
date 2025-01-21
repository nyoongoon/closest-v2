package com.example.closestv2.api.controller;

import com.example.closestv2.api.BlogAuthApi;
import com.example.closestv2.api.usecases.BlogAuthUsecase;
import com.example.closestv2.models.AuthMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class BlogAuthController implements BlogAuthApi {
    private final BlogAuthUsecase blogAuthUsecase;

    @Override
    public ResponseEntity<AuthMessageResponse> blogAuthMessageGet(URI rssUri) {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        String email = (String) authentication.getPrincipal();;
        AuthMessageResponse blogAuthMessage = blogAuthUsecase.createBlogAuthMessage(email, rssUri);
        return ResponseEntity.ok(blogAuthMessage);
    }

    @Override
    public ResponseEntity<Void> blogAuthVerificationPost() {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        String email = (String) authentication.getPrincipal();;
        blogAuthUsecase.verifyBlogAuthMessage(email);
        return ResponseEntity.ok().build();
    }
}

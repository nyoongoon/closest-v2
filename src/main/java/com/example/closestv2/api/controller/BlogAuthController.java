package com.example.closestv2.api.controller;

import com.example.closestv2.api.BlogAuthApi;
import com.example.closestv2.api.usecases.BlogAuthUsecase;
import com.example.closestv2.models.AuthMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class BlogAuthController implements BlogAuthApi {
    private final BlogAuthUsecase blogAuthUsecase;

    @Override
    public ResponseEntity<AuthMessageResponse> blogAuthMessageGet(URI rssUri) {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        AuthMessageResponse blogAuthMessage = blogAuthUsecase.createBlogAuthMessage(user.getUsername(), rssUri);
        return ResponseEntity.ok(blogAuthMessage);
    }

    @Override
    public ResponseEntity<Void> blogAuthVerificationPost() {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        blogAuthUsecase.verifyBlogAuthMessage(user.getUsername());
        return ResponseEntity.ok().build();
    }
}

package com.example.closestv2.api.controller;

import com.example.closestv2.api.PostLikeApi;
import com.example.closestv2.api.service.model.request.PostsLikePostServiceRequest;
import com.example.closestv2.api.usecases.PostLikeUsecase;
import com.example.closestv2.models.PostsLikePostRequest;
import com.example.closestv2.util.url.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostLikeController implements PostLikeApi {
    private PostLikeUsecase postLikeUsecase;

    @Override
    public ResponseEntity<Void> postsLikePost(PostsLikePostRequest request) {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        PostsLikePostServiceRequest serviceRequest = toServiceRequest((long) principal, request);
        postLikeUsecase.likePost(serviceRequest);
        return ResponseEntity.ok().build();
    }

    private PostsLikePostServiceRequest toServiceRequest(long memberId, PostsLikePostRequest request) {
        return PostsLikePostServiceRequest.builder()
                .memberId(memberId)
                .postUrl(UrlUtils.from(request.getPostUri()))
                .build();

    }
}

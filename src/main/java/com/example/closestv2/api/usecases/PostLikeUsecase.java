package com.example.closestv2.api.usecases;

import com.example.closestv2.api.service.model.request.PostsLikePostServiceRequest;
import org.springframework.stereotype.Service;

@Service
public interface PostLikeUsecase {
    void likePost(PostsLikePostServiceRequest serviceRequest);
}

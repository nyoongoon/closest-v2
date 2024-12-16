package com.example.closestv2.api.usecases;

import com.example.closestv2.models.AuthMessageResponse;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public interface BlogAuthUsecase {
    AuthMessageResponse createBlogAuthMessage(long memberId, URI ussUri);

    void verifyBlogAuthMessage(long memberId);
}

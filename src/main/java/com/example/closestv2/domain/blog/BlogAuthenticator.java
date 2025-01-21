package com.example.closestv2.domain.blog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class BlogAuthenticator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public BlogAuthCode createAuthCode(String memberEmail, URL rssUrl) {
        String authMessage = generateRandomAuthMessage();
        return new BlogAuthCode(memberEmail, rssUrl, authMessage);
    }

    public boolean authenticate(BlogAuthCode blogAuthCode, String blogTitle) {
        String authMessage = blogAuthCode.authMessage();
        return authMessage.equals(blogTitle);
    }

    private String generateRandomAuthMessage() {
        StringBuilder result = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(index));
        }
        return result.toString();
    }
}

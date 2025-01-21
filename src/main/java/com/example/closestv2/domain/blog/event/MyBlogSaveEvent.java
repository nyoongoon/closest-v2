package com.example.closestv2.domain.blog.event;

import java.net.URL;

public record MyBlogSaveEvent(
        String memberEmail,
        URL blogUrl
) {
}

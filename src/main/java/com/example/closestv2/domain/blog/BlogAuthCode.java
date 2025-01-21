package com.example.closestv2.domain.blog;

import java.net.URL;

public record BlogAuthCode(
        String memberEmail,
        URL rssUrl,
        String authMessage
) {
}

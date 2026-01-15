package com.example.closestv2.util.url;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.*;

public final class UrlUtils {
    private UrlUtils() {
        throw new IllegalStateException(CANNOT_INSTANTIATE);
    }

    public static URL from(String uriStr) {
        URI uri = URI.create(uriStr);
        return from(uri);
    }

    public static URL from(URI uri) {
        URL url;
        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(WRONG_RSS_URL_FORMAT);
        }
        return url;
    }

    public static URI toUri(URL url) {
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(SERVER_ERROR);
        }
        return uri;
    }
}

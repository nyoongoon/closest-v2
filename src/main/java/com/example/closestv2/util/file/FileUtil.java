package com.example.closestv2.util.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtil {
    public static String readFileAsString(String fileName) throws IOException {
        // ClassLoader를 사용해 클래스패스 기준으로 파일 위치를 가져옴
        try {
            java.net.URL resource = FileUtil.class.getClassLoader().getResource(fileName);
            if (resource == null) {
                throw new IOException("File not found: " + fileName);
            }
            return Files.readString(Paths.get(resource.toURI()));
        } catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
    }
}

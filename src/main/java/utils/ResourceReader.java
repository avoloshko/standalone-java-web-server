package utils;

import java.io.InputStream;

public class ResourceReader {

    public static InputStream readStream(ClassLoader classLoader, String fileName) {
        return classLoader.getResourceAsStream(fileName);
    }
}

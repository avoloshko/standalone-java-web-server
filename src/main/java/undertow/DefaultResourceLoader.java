package undertow;

import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.URLResource;

import java.io.IOException;
import java.net.URL;


public class DefaultResourceLoader extends ClassPathResourceManager {
    public DefaultResourceLoader(ClassLoader classLoader) {
        super(classLoader, "");
    }

    @Override
    public Resource getResource(String path) throws IOException {
        if (path.equals("/")) {
            // fixes the case when packed into JAR
            URL resource = new URL("file:" + System.getProperty("user.dir"));
            return new URLResource(resource, path);
        }
        return super.getResource(path);
    }
}

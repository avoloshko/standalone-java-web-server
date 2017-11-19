import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLClassLoader;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        logger.info("App is starting up...");

        AppServer server = new AppServer((URLClassLoader) Thread.currentThread().getContextClassLoader());
        server.start("localhost", 3000);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("App is shutting down...");
            server.stop();
        }));
    }
}

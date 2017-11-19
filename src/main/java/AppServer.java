import dorkbox.annotation.AnnotationDefaults;
import dorkbox.annotation.AnnotationDetector;
import io.undertow.Undertow;
import io.undertow.jsp.HackInstanceManager;
import io.undertow.jsp.JspServletBuilder;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.servlet.api.*;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import undertow.DefaultResourceLoader;
import undertow.TldLocator;
import undertow.WebXMLParser;
import utils.ResourceReader;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

public class AppServer {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private Undertow undertow;
    private DeploymentManager deploymentManager;
    private URLClassLoader classLoader;

    AppServer(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    HttpHandler handler() {
        final PathHandler pathHandler = new PathHandler();
        HttpHandler encodingHandler = new EncodingHandler.Builder().build(null)
                .wrap(pathHandler);
        return encodingHandler;
    }

    void start(String host, int port) throws Exception {
        if (undertow != null) {
            throw new IllegalStateException();
        }

        final PathHandler pathHandler = new PathHandler();
        HttpHandler encodingHandler = new EncodingHandler.Builder().build(null)
                .wrap(pathHandler);

        final ServletContainer container = ServletContainer.Factory.newInstance();

        WebXMLParser webXMLParser = new WebXMLParser();
        DeploymentInfo builder = webXMLParser.parse(ResourceReader.readStream(classLoader,"WEB-INF/web.xml"));
        builder.setClassLoader(classLoader)
                .setContextPath("/")
                .setDeploymentName(AppServer.class.getSimpleName())
                .setResourceManager(new DefaultResourceLoader(classLoader))
                .addServlets(findWebServlets())
                .addListeners(findWebListeners());

        if (webXMLParser.isJspEnabled()) {
            HashMap<String, TagLibraryInfo> tagLibraryInfo = TldLocator.createTldInfos(classLoader);
            tagLibraryInfo.putAll(TldLocator.createTldInfos(classLoader, builder.getJspConfigDescriptor().getTaglibs()));

            JspServletBuilder.setupDeployment(builder, new HashMap<>(), tagLibraryInfo, new HackInstanceManager());
        }

        deploymentManager = container.addDeployment(builder);
        deploymentManager.deploy();

        pathHandler.addPrefixPath(builder.getContextPath(), deploymentManager.start());

        undertow = Undertow.builder().addHttpListener(port, host)
                .setHandler(encodingHandler)
                .build();

        undertow.start();
    }

    private ServletInfo[] findWebServlets() throws IOException {

        List<ServletInfo> result = new ArrayList<>();


        for (Class<?> cls : AnnotationDetector
                .scanClassPath(classLoader, "servlets")
                .forAnnotations(WebServlet.class)
                .collect(AnnotationDefaults.getType)) {
            WebServlet annotation = cls.getAnnotation(WebServlet.class);
            ServletInfo servletInfo = new ServletInfo(annotation.name(), cls.asSubclass(Servlet.class));
            servletInfo.setAsyncSupported(annotation.asyncSupported());
            servletInfo.setLoadOnStartup(annotation.loadOnStartup());
            for (String mapping : annotation.urlPatterns())
                servletInfo.addMapping(mapping);
            for (String mapping : annotation.value())
                servletInfo.addMapping(mapping);

            result.add(servletInfo);
        }

        return result.toArray(new ServletInfo[result.size()]);
    }

    private ListenerInfo[] findWebListeners() throws IOException {
        List<ListenerInfo> result = new ArrayList<>();

        for (Class<?> cls : AnnotationDetector
                .scanClassPath(classLoader, "servlets")
                .forAnnotations(WebListener.class)
                .collect(AnnotationDefaults.getType)) {
            ListenerInfo listenerInfo  = new ListenerInfo(cls.asSubclass(EventListener.class));
            result.add(listenerInfo);
        }

        return result.toArray(new ListenerInfo[result.size()]);
    }

    void stop() {
        undertow.stop();
        undertow = null;

        try {
            deploymentManager.stop();
        } catch (ServletException e) {
            logger.warn("DeploymentManager failed to stop", e);
        }
        deploymentManager.undeploy();
        deploymentManager = null;
    }
}

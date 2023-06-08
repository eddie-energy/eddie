package energy.eddie.framework.web;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Inject;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.framework.Env;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;

public class JavalinApp {

    private static final Logger logger = LoggerFactory.getLogger(JavalinApp.class);

    private static final String SRC_MAIN_PREFIX = "./framework/src/main/";

    @Inject
    private Set<JavalinPathHandler> javalinPathHandlers;

    @Inject
    private Set<RegionConnector> regionConnectors;

    private static boolean inDevelopmentMode() {
        return "true".equals(System.getProperty("developmentMode"));
    }

    /**
     * HTTP proxy that adds a CORS enabling header to the upstream response.
     * <p>
     * Enabling CORS could be done by using web framework specific functions, e.g. with
     * <a href="https://javalin.io/plugins/cors">javalin CORS plugin</a>. But doing it at this place while proxying
     * has the benefit that it can be done here for all region connectors consistently at a single place.
     * <p>
     * Enabling CORS is necessary for the eligible party app to use the custom elements provided by the framework and
     * region connectors.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">mdn: Cross-Origin Resource Sharing (CORS)</a>
     */
    public static final class CorsEnablingProxyServlet extends AsyncProxyServlet.Transparent {
        @Override
        protected void onServerResponseHeaders(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
            super.onServerResponseHeaders(clientRequest, proxyResponse, serverResponse);
            proxyResponse.addHeader("Access-Control-Allow-Origin", "*");
        }
    }

    @java.lang.SuppressWarnings("java:S1075") // causes false-positive findings on the lines marked below
    public void init() {
        // Using try-with-resources with the Javalin instance isn't really intuitive, but: Sonar considers using
        // an AutoClosable without ensuring a close to be a major issue. To keep Javalin running the current thread
        // is suspended in a forever-sleep loop below.
        try (var app = Javalin.create(config -> {
            config.staticFiles.add(staticFileConfig -> {
                // Sonar false positive on following line(java:S1075): This externally visible URL path shouldn't be
                // configurable despite Sonar's recommendation.
                staticFileConfig.hostedPath = "/lib";
                staticFileConfig.mimeTypes.add("text/javascript", ".js");
                if (inDevelopmentMode()) {
                    staticFileConfig.directory = SRC_MAIN_PREFIX + "resources/public" + staticFileConfig.hostedPath;
                    staticFileConfig.location = Location.EXTERNAL;
                } else {
                    staticFileConfig.directory = "public/lib";
                    staticFileConfig.location = Location.CLASSPATH;
                }
            });
            var mapper = JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build();
            config.jsonMapper(new JavalinJackson(mapper));
            var baseUrl = Env.PUBLIC_CONTEXT_PATH.get();
            if (null != baseUrl && !baseUrl.isEmpty()) {
                // Sonar false positive on following line(java:S1075): This shouldn't be externally configurable
                // because it already is, this is just a fallback value.
                config.routing.contextPath = "/" + Env.PUBLIC_CONTEXT_PATH.get();
            }
            config.jetty.contextHandlerConfig(sch -> regionConnectors.forEach(rc -> {
                var rcAddr = rc.startWebapp(new InetSocketAddress("localhost", 0), inDevelopmentMode());
                var proxySource = rc.getMetadata().urlPath() + "*";
                var proxyTarget = "http://localhost:" + rcAddr + "/";
                var proxy = new ServletHolder(CorsEnablingProxyServlet.class);
                proxy.setInitParameter("proxyTo", proxyTarget);
                logger.info("Proxying requests for {} to {}", proxySource, proxyTarget);
                sch.addServlet(proxy, proxySource);
            }));
            config.jetty.server(() -> {
                var server = new Server();
                server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.NCSA_FORMAT));
                return server;
            });
        })) {
            app.after(ctx -> ctx.header("Access-Control-Allow-Origin", "*"));
            javalinPathHandlers.forEach(ph -> ph.registerPathHandlers(app));
            app.start(8080);
            while (!Thread.interrupted()) {
                Thread.sleep(Long.MAX_VALUE);
            }
        } catch (InterruptedException e) {
            logger.info("Exiting.");
            Thread.currentThread().interrupt();
        }
    }
}

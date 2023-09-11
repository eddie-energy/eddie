package energy.eddie.framework.web;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Inject;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.framework.Env;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.json.JavalinJackson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

public class JavalinApp {

    private static final Logger logger = LoggerFactory.getLogger(JavalinApp.class);

    private static final String DEVELOPMENT_MODE = "developmentMode";
    private static final int DEFAULT_PORT = 8080;

    private final Boolean devMode;
    private final String baseUrl;
    private final int port;

    private final Set<JavalinPathHandler> javalinPathHandlers;
    private final Set<RegionConnector> regionConnectors;

    @Inject
    public JavalinApp(Config config, Set<JavalinPathHandler> javalinPathHandlers, Set<RegionConnector> regionConnectors) {
        devMode = config.getOptionalValue(DEVELOPMENT_MODE, Boolean.class).orElse(true);
        baseUrl = config.getOptionalValue(Env.PUBLIC_CONTEXT_PATH.name(), String.class).orElse("");
        port = config.getOptionalValue(Env.FRAMEWORK_PORT.name(), Integer.class).orElse(DEFAULT_PORT);
        this.javalinPathHandlers = javalinPathHandlers;
        this.regionConnectors = regionConnectors;
    }

    private boolean inDevelopmentMode() {
        return devMode;
    }

    @java.lang.SuppressWarnings("java:S1075") // causes false-positive findings on the lines marked below
    public void init() {
        // Using try-with-resources with the Javalin instance isn't really intuitive, but: Sonar considers using
        // an AutoClosable without ensuring a close to be a major issue. To keep Javalin running the current thread
        // is suspended in a forever-sleep loop below.
        try (var app = Javalin.create(config -> {
            var mapper = JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build();
            config.jsonMapper(new JavalinJackson(mapper));

            config.routing.contextPath = baseUrl;
            config.jetty.contextHandlerConfig(servletContextHandler -> {
                regionConnectors.forEach(rc -> {
                    var regionConnectorAddress = rc.startWebapp(new InetSocketAddress("localhost", 0), inDevelopmentMode());
                    proxy(servletContextHandler, rc.getMetadata().urlPath() + "*", "http://localhost:" + regionConnectorAddress + "/");
                });
                proxy(servletContextHandler, "/api/data-needs/*", "http://localhost:8079/");
            });
            config.jetty.server(() -> {
                var server = new Server();
                server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.NCSA_FORMAT));
                return server;
            });

            config.jetty.wsFactoryConfig(wsConfig -> wsConfig.setIdleTimeout(Duration.ofHours(1)));
        })) {

            app.get("/lib/eddie-components.js", context -> {
                context.contentType(ContentType.TEXT_JS);
                context.result(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("public/lib/eddie-components.js")));
            });

            app.after(ctx -> ctx.header("Access-Control-Allow-Origin", "*"));

            javalinPathHandlers.forEach(ph -> ph.registerPathHandlers(app));

            app.start(port);

            while (!Thread.interrupted()) {
                Thread.sleep(Long.MAX_VALUE);
            }


        } catch (InterruptedException e) {
            logger.info("Exiting.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Configures the servletContextHandler to proxy requests from the sourcePath to the proxTarget.
     *
     * @param servletContextHandler Jetty's ServletContextHandler
     * @param proxySource           wildcarded path to select the requests to be proxied, e.g. /api/*
     * @param proxyTarget           HTTP URL proxy target, e.g. http://localhost:8079/
     */
    private static void proxy(ServletContextHandler servletContextHandler, String proxySource, String proxyTarget) {
        logger.info("Proxying requests for {} to {}", proxySource, proxyTarget);
        var proxy = new ServletHolder(CorsEnablingProxyServlet.class);
        proxy.setInitParameter("proxyTo", proxyTarget);
        servletContextHandler.addServlet(proxy, proxySource);
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
}

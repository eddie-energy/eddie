package energy.eddie.examples.exampleapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class ExampleApp {

    public static final String SRC_MAIN_PREFIX = "./src/main/";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleApp.class);

    private static boolean inDevelopmentMode() {
        return "true".equals(System.getProperty("developmentMode"));
    }

    public static void main(String[] args) {
        if (inDevelopmentMode()) {
            LOGGER.info("Executing JteTemplates in development mode");
            var resolver = new DirectoryCodeResolver(Path.of(SRC_MAIN_PREFIX, "jte"));
            JavalinJte.init(TemplateEngine.create(resolver, ContentType.Html));
        } else {
            JavalinJte.init(TemplateEngine.createPrecompiled(Path.of("jte-classes"), ContentType.Html));
        }
        // Using try-with-resources with the Javalin instance isn't really intuitive, but: Sonar considers using
        // an AutoClosable without ensuring a close to be a major issue. To keep Javalin running the current thread
        // is suspended in a forever-sleep loop below.
        try (var app = Javalin.create(config -> {
            config.requestLogger.http((ctx, executionTimeMs) ->
                    LOGGER.info("{} {} -> {} {}ms", ctx.method(), ctx.url(), ctx.statusCode(), executionTimeMs));
            var baseUrl = Env.PUBLIC_CONTEXT_PATH.get();
            if (null != baseUrl && !baseUrl.isEmpty()) {
                config.routing.contextPath = Env.PUBLIC_CONTEXT_PATH.get();
            }
        })) {
            app.before(ctx -> {
                var path = ctx.path().substring(ctx.contextPath().length());
                if (null == ctx.sessionAttribute("user") && !path.startsWith("/login")) {
                    var dest = ctx.contextPath() + "/login";
                    LOGGER.info("User isn't logged in, redirecting to {}", dest);
                    ctx.redirect(dest);
                }
            });

            app.get("/", ctx -> ctx.redirect("login"));
            var injector = Guice.createInjector(new Module());

            List.of(LoginHandler.class, ShowConnectionListHandler.class, ShowConnectionHandler.class).stream()
                    .map(injector::getInstance)
                    .forEach(handler -> handler.register(app));
            app.start(8081);
            while (!Thread.interrupted()) {
                Thread.sleep(Long.MAX_VALUE);
            }
        } catch (InterruptedException e) {
            LOGGER.info("Exiting.");
            Thread.currentThread().interrupt();
        }
    }

    private static class Module extends AbstractModule {
        @Override
        protected void configure() {
            bind(ObjectMapper.class).toInstance(JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build());
            var jdbcUserName = Env.JDBC_USER.get();
            var jdbcPassword = Env.JDBC_PASSWORD.get();
            if (jdbcUserName != null && jdbcPassword != null) {
                bind(Jdbi.class).toInstance(Jdbi.create(Env.JDBC_URL.get(), jdbcUserName, jdbcPassword));
            } else {
                bind(Jdbi.class).toInstance(Jdbi.create(Env.JDBC_URL.get()));
            }
        }
    }

}
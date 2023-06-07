package energy.eddie.epdemoapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinJte;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class EpDemoApp {

    private static final Logger logger = LoggerFactory.getLogger(EpDemoApp.class);

    public static final String SRC_MAIN_PREFIX = "./src/main/";

    private static boolean inDevelopmentMode() {
        return "true".equals(System.getProperty("developmentMode"));
    }

    private static class Module extends AbstractModule {
        @Override
        protected void configure() {
            bind(ObjectMapper.class).toInstance(JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build());
            var jdbcUserName = Env.JDBC_USER.get();
            var jdbcPassword = Env.JDBC_PASSWORD.get();
            if (null != jdbcUserName && null != jdbcPassword) {
                bind(Jdbi.class).toInstance(Jdbi.create(Env.JDBC_URL.get(), jdbcUserName, jdbcPassword));
            } else {
                bind(Jdbi.class).toInstance(Jdbi.create(Env.JDBC_URL.get()));
            }
        }
    }

    public static void main(String[] args) {
        if (inDevelopmentMode()) {
            logger.info("executing JteTemplates in development mode");
            var resolver = new DirectoryCodeResolver(Path.of(SRC_MAIN_PREFIX, "jte"));
            JavalinJte.init(TemplateEngine.create(resolver, ContentType.Html));
        } else {
            JavalinJte.init(TemplateEngine.createPrecompiled(Path.of("jte-classes"), ContentType.Html));
        }

        try (var app = Javalin.create(config -> {
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/";
                if (inDevelopmentMode()) {
                    staticFileConfig.directory = SRC_MAIN_PREFIX + "resources/public";
                    staticFileConfig.location = Location.EXTERNAL;
                } else {
                    staticFileConfig.directory = "public";
                    staticFileConfig.location = Location.CLASSPATH;
                }
            });
            config.requestLogger.http((ctx, executionTimeMs) ->
                    logger.info("{} {} -> {} {}ms", ctx.method(), ctx.url(), ctx.statusCode(), executionTimeMs));
            var baseUrl = Env.PUBLIC_CONTEXT_PATH.get();
            if (null != baseUrl && !baseUrl.isEmpty()) {
                config.routing.contextPath = Env.PUBLIC_CONTEXT_PATH.get();
            }
        })) {
            app.before(ctx -> {
                var path = ctx.path().substring(ctx.contextPath().length());
                if (null == ctx.sessionAttribute("user") && !path.startsWith("/login")) {
                    var dest = ctx.contextPath() + "/login";
                    logger.info("user isn't logged in, redirecting to {}", dest);
                    ctx.redirect(dest);
                }
            });
            app.get("/", ctx -> ctx.redirect("login"));
            var injector = Guice.createInjector(new Module());

            List.of(LoginHandler.class, ShowConnectionListHandler.class, ShowConnectionHandler.class).stream()
                    .map(injector::getInstance)
                    .forEach(handler -> handler.register(app));
            app.start(8081);
        }
    }
}
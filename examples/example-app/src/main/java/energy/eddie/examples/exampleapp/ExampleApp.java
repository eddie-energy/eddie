package energy.eddie.examples.exampleapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageMixin;
import energy.eddie.examples.exampleapp.kafka.KafkaListener;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class ExampleApp {

    public static final String SRC_MAIN_PREFIX = "./src/main/";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleApp.class);

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        Flyway flyway = Flyway.configure()
                              .baselineOnMigrate(true)
                              .dataSource(Env.JDBC_URL.get(), Env.JDBC_USER.get(), Env.JDBC_PASSWORD.get())
                              .locations("db/migration")
                              .load();
        flyway.migrate();

        var injector = Guice.createInjector(new Module());
        JavalinJte jte;
        if (inDevelopmentMode()) {
            LOGGER.info("Executing JteTemplates in development mode");
            var resolver = new DirectoryCodeResolver(Path.of(SRC_MAIN_PREFIX, "jte"));
            jte = new JavalinJte(TemplateEngine.create(resolver, ContentType.Html));
        } else {
            jte = new JavalinJte(TemplateEngine.createPrecompiled(Path.of("jte-classes"), ContentType.Html));
        }

        var kafkaListener = new KafkaListener(injector.getInstance(Jdbi.class),
                                              injector.getInstance(ObjectMapper.class));
        var executor = Executors.newSingleThreadExecutor();
        awaitResultAsync(executor.submit(kafkaListener));
        var app = Javalin.create(config -> {
            config.requestLogger.http((ctx, executionTimeMs) ->
                                              LOGGER.info("{} {} -> {} {}ms",
                                                          ctx.method(),
                                                          ctx.url(),
                                                          ctx.statusCode(),
                                                          executionTimeMs));
            var baseUrl = Env.PUBLIC_CONTEXT_PATH.get();
            if (null != baseUrl && !baseUrl.isEmpty()) {
                config.router.contextPath = Env.PUBLIC_CONTEXT_PATH.get();
            }
            config.fileRenderer(jte);
        });
        app.before(ctx -> {
            var path = ctx.path().substring(ctx.contextPath().length());
            if (null == ctx.sessionAttribute("user") && !path.startsWith("/login")) {
                var dest = ctx.contextPath() + "/login";
                LOGGER.info("User isn't logged in, redirecting to {}", dest);
                ctx.redirect(dest);
            }
        });

        app.get("/", ctx -> ctx.redirect("login"));

        Stream.of(LoginHandler.class, ShowConnectionListHandler.class, ShowConnectionHandler.class)
              .map(injector::getInstance)
              .forEach(handler -> handler.register(app));
        app.start(8081);
    }

    private static void awaitResultAsync(Future<?> future) {
        Thread.startVirtualThread(() -> {
            try {
                future.get();
            } catch (InterruptedException e) {
                LOGGER.warn("Error executing kafka", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOGGER.warn("Error executing kafka", e);
            }
        });
    }

    private static boolean inDevelopmentMode() {
        return "true".equals(System.getProperty("developmentMode"));
    }

    private static class Module extends AbstractModule {
        @Override
        protected void configure() {
            bind(ObjectMapper.class).toInstance(JsonMapper.builder()
                                                          .addModule(new JavaTimeModule())
                                                          .addModule(new Jdk8Module())
                                                          .addModule(new JakartaXmlBindAnnotationModule())
                                                          .addMixIn(ConnectionStatusMessage.class,
                                                                    ConnectionStatusMessageMixin.class)
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

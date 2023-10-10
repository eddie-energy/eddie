package energy.eddie.core.web;

import io.javalin.Javalin;

public interface JavalinPathHandler {
    void registerPathHandlers(Javalin app);
}

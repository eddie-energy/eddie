package energy.eddie.framework.web;

import io.javalin.Javalin;

public interface JavalinPathHandler {
    void registerPathHandlers(Javalin app);
}

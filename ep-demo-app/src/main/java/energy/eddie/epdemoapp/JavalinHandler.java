package energy.eddie.epdemoapp;

import io.javalin.Javalin;

public interface JavalinHandler {
    void register(Javalin app);
}

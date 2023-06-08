package energy.eddie.examples.exampleapp;

import io.javalin.Javalin;

public interface JavalinHandler {
    void register(Javalin app);
}

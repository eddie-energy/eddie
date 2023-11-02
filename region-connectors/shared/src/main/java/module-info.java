module energy.eddie.region.connector.shared {
    requires energy.eddie.api;
    requires reactor.core;
    requires io.javalin;
    requires jakarta.annotation;
    requires kotlin.stdlib;
    exports energy.eddie.regionconnector.shared.permission.requests.decorators;
    exports energy.eddie.regionconnector.shared.utils;
}
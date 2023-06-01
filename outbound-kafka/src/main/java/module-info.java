module energy.eddie.outbound.kafka {
    exports energy.eddie.outbound.kafka;

    requires energy.eddie.api;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;
    requires reactor.core;
    requires kafka.clients;
    requires java.compiler;
}
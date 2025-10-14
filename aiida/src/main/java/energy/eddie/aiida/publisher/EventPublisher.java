package energy.eddie.aiida.publisher;

public interface EventPublisher<T> {
    void publishEvent(T event);
}

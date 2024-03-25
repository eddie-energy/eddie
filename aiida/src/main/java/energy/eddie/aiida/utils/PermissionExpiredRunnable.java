package energy.eddie.aiida.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.time.Instant;

public class PermissionExpiredRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionExpiredRunnable.class);
    private final String permissionId;
    private final Instant expirationTime;
    private final Sinks.One<String> expirationSink;

    /**
     * Create a new runnable that will notify the {@code listener} when it runs.
     *
     * @param permissionId   ID that should be passed to the listener on expiration
     * @param expirationTime UTC timestamp when the permission should expire. Just used to calculate and print the offset,
     *                       between the target time and when this Runnable is actually run.
     * @param expirationSink Sink of a {@code Mono<String>} on which the permissionId will be published when the runnable runs
     */
    public PermissionExpiredRunnable(String permissionId, Instant expirationTime, Sinks.One<String> expirationSink) {
        this.permissionId = permissionId;
        this.expirationTime = expirationTime;
        this.expirationSink = expirationSink;
    }

    @Override
    public void run() {
        var offsetMs = Instant.now().toEpochMilli() - expirationTime.toEpochMilli();
        LOGGER.info("ExpirePermissionRunnable running for permission {} with offset of {} ms to target time (negative number would be too early)", permissionId, offsetMs);
        Sinks.EmitResult result = expirationSink.tryEmitValue(permissionId);

        if (result.isFailure()) {
            LOGGER.error("Error while trying to emit expiration signal for permission {}. Error was: {}", permissionId, result);
        }
    }
}

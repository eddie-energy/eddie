package energy.eddie.outbound.rest.model;

import java.util.ArrayList;
import java.util.List;

public interface ModelWithJsonPayload<T> {
    static <T1 extends ModelWithJsonPayload<T2>, T2> List<T2> payloadsOf(List<T1> all) {
        var messages = new ArrayList<T2>();
        for (var model : all) {
            var payload = model.payload();
            messages.add(payload);
        }
        return messages;
    }

    T payload();
}

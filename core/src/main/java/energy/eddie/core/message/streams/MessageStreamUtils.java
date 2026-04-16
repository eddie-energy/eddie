// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.message.streams;

import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

public class MessageStreamUtils {

    private MessageStreamUtils() {
        // Utility class
    }

    public static boolean isProviderMethod(Method method) {
        return Flux.class.isAssignableFrom(method.getReturnType())
               && method.getParameterCount() == 0;
    }

    public static boolean isReceiverMethod(Method method) {
        return method.getParameterCount() == 1
               && Flux.class.isAssignableFrom(method.getParameterTypes()[0])
               && method.getReturnType() == void.class;
    }
}

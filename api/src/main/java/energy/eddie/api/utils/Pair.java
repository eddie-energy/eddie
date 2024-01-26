package energy.eddie.api.utils;

import jakarta.annotation.Nullable;

public record Pair<K, V>(@Nullable K key, V value) {
}

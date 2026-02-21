package energy.eddie.regionconnector.de.eta.oauth;

import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthCallbackTest {

    @Test
    void constructorWhenBothCodeAndErrorEmptyShouldThrowException() {
        Optional<String> emptyCode = Optional.empty();
        Optional<String> emptyError = Optional.empty();
        assertThatThrownBy(() -> new OAuthCallback(emptyCode, emptyError, "state"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Either code or error must be provided");
    }

    @Test
    void constructorWhenBothCodeAndErrorPresentShouldThrowException() {
        Optional<String> code = Optional.of("code");
        Optional<String> error = Optional.of("error");
        assertThatThrownBy(() -> new OAuthCallback(code, error, "state"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only one of code or error must be provided");
    }

    @Test
    void isSuccessfulWhenCodePresentShouldReturnTrue() {
        OAuthCallback callback = new OAuthCallback(Optional.of("code"), Optional.empty(), "state");
        assertThat(callback.isSuccessful()).isTrue();
    }

    @Test
    void isSuccessfulWhenErrorPresentShouldReturnFalse() {
        OAuthCallback callback = new OAuthCallback(Optional.empty(), Optional.of("error"), "state");
        assertThat(callback.isSuccessful()).isFalse();
    }
}

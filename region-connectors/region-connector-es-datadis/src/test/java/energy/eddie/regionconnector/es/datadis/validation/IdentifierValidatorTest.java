package energy.eddie.regionconnector.es.datadis.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"K0000000T", "L0000000T", "M0000000T", "X0000000T", "Y0000000Z", "00000000-t", "00000000 T"})
    @MethodSource("eightNumberNifSupplier")
    void givenValidNif_returnsTrue(String nif) {
        // Given
        var validator = new IdentifierValidator();

        // When
        var res = validator.isValidIdentifier(nif);

        // Then
        assertTrue(res);
    }

    @ParameterizedTest
    @ValueSource(strings = {"G58818501", "W5881850A"})
    void givenValidCif_returnsTrue(String cif) {
        // Given
        var validator = new IdentifierValidator();

        // When
        var res = validator.isValidIdentifier(cif);

        // Then
        assertTrue(res);
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000001T", "L0000001T", "Y0000000T", "W58818501", "G5881850A"})
    void givenInvalidIdentifier_returnsFalse(String cif) {
        // Given
        var validator = new IdentifierValidator();

        // When
        var res = validator.isValidIdentifier(cif);

        // Then
        assertFalse(res);
    }

    private static Stream<Arguments> eightNumberNifSupplier() {
        var nifLetters = IdentifierValidator.NIF_SECURITY_LETTERS;
        return IntStream.range(0, nifLetters.length())
                        .mapToObj(i -> (i > 9 ? "000000" + i : "0000000" + i) + nifLetters.charAt(i))
                        .map(Arguments::of);
    }
}
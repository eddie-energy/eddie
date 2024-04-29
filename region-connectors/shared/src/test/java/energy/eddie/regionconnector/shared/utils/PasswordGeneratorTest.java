package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {
    private final PasswordGenerator generator = new PasswordGenerator(new SecureRandom());

    @Test
    void givenInvalidLength_throws() {
        assertThrows(IllegalArgumentException.class, () -> generator.generatePassword(-1));
        assertThrows(IllegalArgumentException.class, () -> generator.generatePassword(5));
        assertThrows(IllegalArgumentException.class, () -> generator.generatePassword(11));
        assertThrows(IllegalArgumentException.class, () -> generator.generatePassword(-1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> generator.generatePassword(5, 3, 3, 3, 3));
    }

    @Test
    void generatePassword_containsExactlyNChars() {
        assertEquals(16, generator.generatePassword(16).length());
    }

    @Test
    void generatePassword_containsExactly3CharsOfAllTypes() {
        // When
        var password = generator.generatePassword(12);

        // Then
        assertEquals(3, password.chars()
                                .filter(c -> PasswordGenerator.LOWER_CASE_CHARS.indexOf(c) != -1)
                                .count());
        assertEquals(3, password.chars()
                                .filter(c -> PasswordGenerator.UPPER_CASE_CHARS.indexOf(c) != -1)
                                .count());
        assertEquals(3, password.chars()
                                .filter(c -> PasswordGenerator.SPECIAL_CHARS.indexOf(c) != -1)
                                .count());
        assertEquals(3, password.chars()
                                .filter(c -> PasswordGenerator.DIGIT_CHARS.indexOf(c) != -1)
                                .count());
    }

    @Test
    void generatePassword_containsAtLeast3CharsOfAllTypes() {
        // When
        var password = generator.generatePassword(12);

        // Then
        assertTrue(password.chars().filter(c -> PasswordGenerator.LOWER_CASE_CHARS.indexOf(c) != -1).count() >= 3);
        assertTrue(password.chars().filter(c -> PasswordGenerator.UPPER_CASE_CHARS.indexOf(c) != -1).count() >= 3);
        assertTrue(password.chars().filter(c -> PasswordGenerator.SPECIAL_CHARS.indexOf(c) != -1).count() >= 3);
        assertTrue(password.chars().filter(c -> PasswordGenerator.DIGIT_CHARS.indexOf(c) != -1).count() >= 3);
    }
}

package energy.eddie.regionconnector.shared.utils;

import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.stream.IntStream;

public class PasswordGenerator {
    protected static final String SPECIAL_CHARS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    protected static final String LOWER_CASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    protected static final String UPPER_CASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    protected static final String DIGIT_CHARS = "0123456789";
    private static final String ALL_CHARS = SPECIAL_CHARS + LOWER_CASE_CHARS + UPPER_CASE_CHARS + DIGIT_CHARS;
    private final SecureRandom secureRandom;

    public PasswordGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    /**
     * Creates a random password using a {@link SecureRandom} that contains at least 3 special, 3 lowercase and 3
     * uppercase characters as well as at least 3 digits. The password will consist of only ASCII characters.
     *
     * @param length Total length of the password, must be &gt;= 12.
     */
    public String generatePassword(int length) {
        if (length < 12)
            throw new IllegalArgumentException("Minimum length is 12 characters");

        return generatePassword(length, 3, 3, 3, 3);
    }

    /**
     * @param length Total length of the password, must be greater than 0.
     * @see #generatePassword(int)
     */
    public String generatePassword(int length, int minSpecial, int minLowercase, int minUppercase, int minDigits) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0");
        }

        if (minSpecial + minLowercase + minUppercase + minDigits > length) {
            throw new IllegalArgumentException("Sum of all min character variables must be >= length");
        }

        CharBuffer buffer = CharBuffer.allocate(length);

        appendRandomCharactersFromString(minSpecial, SPECIAL_CHARS, buffer);
        appendRandomCharactersFromString(minLowercase, LOWER_CASE_CHARS, buffer);
        appendRandomCharactersFromString(minUppercase, UPPER_CASE_CHARS, buffer);
        appendRandomCharactersFromString(minDigits, DIGIT_CHARS, buffer);
        appendRandomCharactersFromString(length - buffer.position(), ALL_CHARS, buffer);

        buffer.flip();
        shuffle(buffer);

        return buffer.toString();
    }


    /**
     * Shuffles the content of the <code>buffer</code> by traversing the buffer and swapping the element at the current
     * index with a randomly selected element from the buffer.
     *
     * @param buffer The {@link CharBuffer} containing the characters to be shuffled.
     */
    private void shuffle(CharBuffer buffer) {
        char character;
        int randomIndex;
        for (int i = buffer.position(); i < buffer.limit(); ++i) {
            randomIndex = secureRandom.nextInt(buffer.length());
            character = buffer.get(randomIndex);
            buffer.put(randomIndex, buffer.get(i));
            buffer.put(i, character);
        }
    }

    /**
     * Appends {@code n} characters taken randomly from the supplied {@code source} and appends them at the end of the
     * supplied {@code buffer}.
     */
    private void appendRandomCharactersFromString(int n, String source, CharBuffer buffer) {
        IntStream.range(0, n)
                 .mapToObj(u -> secureRandom.nextInt(source.length()))
                 .map(source::charAt)
                 .forEach(buffer::append);
    }
}

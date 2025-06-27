package energy.eddie.regionconnector.es.datadis.validation;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdentifierValidator {

    static final String NIF_SECURITY_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    static final String CIF_SECURITY_LETTERS = "JABCDEFGHI";
    static final String NUMBER_ONLY_CIF_TYPES = "ABCDEFGHJUV";

    /**
     * Checks if the identifier is a valid NIF, NIE, DNI, or CIF.
     *
     * @param identifier a NIF or CIF string
     * @return true if valid, false otherwise
     */
    public boolean isValidIdentifier(String identifier) {
        identifier = identifier.replace(" ", "")
                               .replace("-", "")
                               .toUpperCase(Locale.ROOT);
        return isNif(identifier) || isCif(identifier);
    }

    /**
     * Validates NIF (including DNI, NIE, and special cases like KLM).
     *
     * @param nif NIF string to validate
     * @return true if valid NIF, false otherwise
     * @see <a href="https://es.wikipedia.org/wiki/N%C3%BAmero_de_identificaci%C3%B3n_fiscal#NIF_de_personas_f%C3%ADsicas">Wikipedia - NIF</a>
     */
    private boolean isNif(String nif) {
        // DNI
        Pattern dniPattern = Pattern.compile("^(\\d{8})([A-HJ-NP-TV-Z])$");
        Matcher dniMatcher = dniPattern.matcher(nif);
        if (dniMatcher.matches()) {
            int number = Integer.parseInt(dniMatcher.group(1));
            return NIF_SECURITY_LETTERS.charAt(number % 23) == dniMatcher.group(2).charAt(0);
        }

        // KLM NIF
        Pattern klmPattern = Pattern.compile("^[KLM](\\d{7})([A-HJ-NP-TV-Z])$");
        Matcher klmMatcher = klmPattern.matcher(nif);
        if (klmMatcher.matches()) {
            int number = Integer.parseInt(klmMatcher.group(1));
            return NIF_SECURITY_LETTERS.charAt(number % 23) == klmMatcher.group(2).charAt(0);
        }

        // NIE
        Pattern niePattern = Pattern.compile("^([XYZ])(\\d{7})([A-HJ-NP-TV-Z])$");
        Matcher nieMatcher = niePattern.matcher(nif);
        if (nieMatcher.matches()) {
            int prefixValue = nieMatcher.group(1).charAt(0) - 'X';
            String numericPart = prefixValue + nieMatcher.group(2);
            int number = Integer.parseInt(numericPart);
            return NIF_SECURITY_LETTERS.charAt(number % 23) == nieMatcher.group(3).charAt(0);
        }

        return false;
    }

    /**
     * Validates CIF (including both digit and letter check characters).
     *
     * @param cif CIF string to validate
     * @return true if valid CIF, false otherwise
     * @see <a href="https://es.wikipedia.org/wiki/C%C3%B3digo_de_identificaci%C3%B3n_fiscal">Wikipedia - CIF</a>
     */
    private boolean isCif(String cif) {
        Pattern cifPattern = Pattern.compile("^([ABCDEFGHJNPQRSUVW])(\\d{7})([A-Z\\d])$");
        Matcher matcher = cifPattern.matcher(cif);
        if (!matcher.matches()) {
            return false;
        }

        String type = matcher.group(1);
        String digits = matcher.group(2);
        String control = matcher.group(3);

        int evenSum = 0;
        int oddSum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int num = parseCharToInt(digits.charAt(i));
            if ((i + 1) % 2 == 0) {
                evenSum += num;
            } else {
                int n = num * 2;
                int sum = String.valueOf(n).chars()
                                .map(IdentifierValidator::parseCharToInt)
                                .sum();
                oddSum += sum;
            }
        }

        int total = evenSum + oddSum;
        int checkDigit = (10 - (total % 10)) % 10;
        char expectedLetter = CIF_SECURITY_LETTERS.charAt(checkDigit);

        boolean expectsDigit = NUMBER_ONLY_CIF_TYPES.contains(type);

        if (expectsDigit) {
            return parseCharToInt(control.charAt(0)) == checkDigit;
        } else {
            return control.charAt(0) == expectedLetter;
        }
    }

    private static int parseCharToInt(char c) {
        return parseCharToInt((int) c);
    }

    private static int parseCharToInt(int c) {
        return c - '0';
    }
}
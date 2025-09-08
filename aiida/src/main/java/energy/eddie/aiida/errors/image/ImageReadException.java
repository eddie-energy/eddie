package energy.eddie.aiida.errors.image;


public class ImageReadException extends Exception {
    public ImageReadException(String filename) {
        super("Cannot read file %s".formatted(filename));
    }
}

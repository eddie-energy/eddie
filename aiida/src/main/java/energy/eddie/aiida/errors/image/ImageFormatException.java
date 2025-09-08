package energy.eddie.aiida.errors.image;


public class ImageFormatException extends Exception {
    public ImageFormatException(String filename) {
        super("Unsupported format for file %s".formatted(filename));
    }
}

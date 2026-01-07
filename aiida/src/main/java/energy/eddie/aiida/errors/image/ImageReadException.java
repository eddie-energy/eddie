package energy.eddie.aiida.errors.image;


public class ImageReadException extends Exception {
    public ImageReadException(String filename, Exception e) {
        super("Cannot read file %s".formatted(filename), e);
    }
}

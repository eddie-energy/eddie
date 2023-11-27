package energy.eddie.regionconnector.shared.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class CustomElementPath {
    private final Class<?> clazz;
    private final String[] devPaths;
    private final String prodPath;

    public CustomElementPath(Class<?> clazz, String[] devPaths, String prodPath) {
        this.clazz = clazz;
        this.devPaths = devPaths;
        this.prodPath = prodPath;
    }


    public InputStream getCEInputStream(boolean dev) throws FileNotFoundException {
        return dev
                ? new FileInputStream(findCEDevPath())
                : Objects.requireNonNull(clazz.getResourceAsStream(prodPath));
    }

    private String findCEDevPath() throws FileNotFoundException {
        for (String ceDevPath : devPaths) {
            if (new File(ceDevPath).exists()) {
                return ceDevPath;
            }
        }
        throw new FileNotFoundException();
    }
}

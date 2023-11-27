package energy.eddie.regionconnector.shared.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CustomElementPathTest {

    @Test
    void testGetCEInputStream_dev_true_using_first_path(@TempDir Path tempDir) {

        // Given
        Class<?> clazz = this.getClass();
        String[] devPaths = {tempDir.resolve("test1").toString(), tempDir.resolve("test2").toString()};
        String prodPath = "dummyPath";

        String content = "This is a test file content";
        try (PrintWriter out = new PrintWriter(devPaths[0], Charset.defaultCharset())) {
            out.print(content);
        } catch (IOException e) {
            fail("Failed to create a test file.");
        }

        CustomElementPath customElementPath = new CustomElementPath(clazz, devPaths, prodPath);

        // When
        try (InputStream inputStream = customElementPath.getCEInputStream(true)) {

            // Then
            assertNotNull(inputStream);
            String fileContent = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset())).readLine();
            assertEquals(content, fileContent);
        } catch (IOException e) {
            fail("Failed to read from the test file.");
        }
    }

    @Test
    void testGetCEInputStream_dev_false() {

        // Given
        Class<?> clazz = this.getClass();
        String[] devPaths = {"dummyPath1", "dummyPath2"};
        String prodPath = "/";
        CustomElementPath customElementPath = new CustomElementPath(clazz, devPaths, prodPath);

        // When
        try (InputStream inputStream = customElementPath.getCEInputStream(false)) {

            // Then
            assertNotNull(inputStream);
        } catch (IOException e) {
            fail("Failed to get the InputStream in production environment.");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testFindDevPaths_noExistsFile() {

        // Given
        Class<?> clazz = this.getClass();
        String[] devPaths = {"nonExistingPath1", "nonExistingPath2"};
        String prodPath = "/";
        CustomElementPath customElementPath = new CustomElementPath(clazz, devPaths, prodPath);

        // When & Then
        assertThrows(FileNotFoundException.class, () -> customElementPath.getCEInputStream(true));
    }
}
// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.image.ImageNotFoundException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.image.Image;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.ImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSourceImageServiceTest {
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private DataSourceRepository dataSourceRepository;

    @InjectMocks
    private DataSourceImageService dataSourceImageService;

    @Test
    void imageByDataSource_shouldReturnImage_whenFound() throws ImageNotFoundException {
        var dataSource = mock(DataSource.class);
        var image = mock(Image.class);
        when(dataSource.image()).thenReturn(image);

        var result = dataSourceImageService.imageByDataSource(dataSource);

        assertEquals(image, result);
    }

    @Test
    void imageByDataSource_shouldThrow_whenImageNotFound() {
        var dataSource = mock(DataSource.class);
        when(dataSource.image()).thenReturn(null);

        assertThrows(ImageNotFoundException.class, () -> dataSourceImageService.imageByDataSource(dataSource));
    }

    @Test
    void updateImage_shouldUpdateImage_whenValid() throws Exception {
        var dataSource = mock(DataSource.class);
        when(dataSource.image()).thenReturn(mock(Image.class));

        MultipartFile file = mock(MultipartFile.class);
        byte[] fileContent = "test".getBytes(StandardCharsets.UTF_8);
        String contentType = "image/png";
        when(file.getContentType()).thenReturn(contentType);
        when(file.getBytes()).thenReturn(fileContent);

        dataSourceImageService.updateImage(dataSource, file);

        verify(imageRepository).delete(any(Image.class));
        verify(dataSource).setImage(argThat(image ->
                Arrays.equals(image.data(), fileContent) && image.contentType().equals(contentType)
        ));
        verify(imageRepository).save(any(Image.class));
        verify(dataSourceRepository).save(dataSource);
    }

    @Test
    void updateImage_shouldNotDeleteImage_whenEmpty() throws Exception {
        var dataSource = mock(DataSource.class);

        MultipartFile file = mock(MultipartFile.class);
        byte[] fileContent = "test".getBytes(StandardCharsets.UTF_8);
        String contentType = "image/png";
        when(file.getContentType()).thenReturn(contentType);
        when(file.getBytes()).thenReturn(fileContent);

        dataSourceImageService.updateImage(dataSource, file);

        verify(imageRepository, never()).delete(any(Image.class));
    }

    @Test
    void updateImage_shouldThrow_whenIOException() throws Exception {
        var dataSource = mock(DataSource.class);
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenThrow(new IOException("Cannot read file"));

        assertThrows(Exception.class, () ->
                dataSourceImageService.updateImage(dataSource, file)
        );
    }

    @Test
    void updateImage_shouldThrow_whenImageFormatInvalid() {
        var dataSource = mock(DataSource.class);
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/plain");

        assertThrows(Exception.class, () ->
                dataSourceImageService.updateImage(dataSource, file)
        );
    }

    @Test
    void deleteImage_shouldDeleteImage_whenFound() throws Exception {
        var dataSource = mock(DataSource.class);
        Image image = mock(Image.class);
        when(dataSource.image()).thenReturn(image);

        dataSourceImageService.deleteImage(dataSource);

        verify(imageRepository).delete(image);
        verify(dataSource).setImage(null);
        verify(dataSourceRepository).save(dataSource);
    }

    @Test
    void deleteImage_shouldThrow_whenImageNotFound() {
        var dataSource = mock(DataSource.class);
        when(dataSource.image()).thenReturn(null);

        assertThrows(ImageNotFoundException.class, () ->
                dataSourceImageService.deleteImage(dataSource)
        );
    }
}

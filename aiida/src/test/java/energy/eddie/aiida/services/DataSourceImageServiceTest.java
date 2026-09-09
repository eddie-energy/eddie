// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSourceImageServiceTest {
    private static final UUID DATA_SOURCE_ID = UUID.randomUUID();

    @Mock
    private ImageRepository imageRepository;
    @Mock
    private DataSourceRepository dataSourceRepository;

    @InjectMocks
    private DataSourceImageService dataSourceImageService;

    @Test
    void imageByDataSourceId_shouldReturnImage_whenFound() throws Exception {
        var dataSource = mock(DataSource.class);
        var image = mock(Image.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        when(dataSource.image()).thenReturn(image);

        var result = dataSourceImageService.imageByDataSourceId(DATA_SOURCE_ID);

        assertEquals(image, result);
    }

    @Test
    void imageByDataSourceId_shouldThrow_whenImageNotFound() {
        var dataSource = mock(DataSource.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        when(dataSource.image()).thenReturn(null);

        assertThrows(
                ImageNotFoundException.class,
                () -> dataSourceImageService.imageByDataSourceId(DATA_SOURCE_ID)
        );
    }

    @Test
    void imageByDataSourceId_shouldThrow_whenDataSourceNotFound() {
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.empty());

        assertThrows(
                DataSourceNotFoundException.class,
                () -> dataSourceImageService.imageByDataSourceId(DATA_SOURCE_ID)
        );
    }

    @Test
    void updateImage_shouldUpdateImage_whenValid() throws Exception {
        var dataSource = mock(DataSource.class);
        var dataSourceImage = mock(Image.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        when(dataSource.image()).thenReturn(dataSourceImage);

        MultipartFile file = mock(MultipartFile.class);
        byte[] fileContent = "test".getBytes(StandardCharsets.UTF_8);
        String contentType = "image/png";
        when(file.getContentType()).thenReturn(contentType);
        when(file.getBytes()).thenReturn(fileContent);

        dataSourceImageService.updateImage(DATA_SOURCE_ID, file);

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
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));

        MultipartFile file = mock(MultipartFile.class);
        byte[] fileContent = "test".getBytes(StandardCharsets.UTF_8);
        String contentType = "image/png";
        when(file.getContentType()).thenReturn(contentType);
        when(file.getBytes()).thenReturn(fileContent);

        dataSourceImageService.updateImage(DATA_SOURCE_ID, file);

        verify(imageRepository, never()).delete(any(Image.class));
    }

    @Test
    void updateImage_shouldThrow_whenIOException() throws Exception {
        var dataSource = mock(DataSource.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenThrow(new IOException("Cannot read file"));

        assertThrows(Exception.class, () ->
                dataSourceImageService.updateImage(DATA_SOURCE_ID, file)
        );
    }

    @Test
    void updateImage_shouldThrow_whenImageFormatInvalid() {
        var dataSource = mock(DataSource.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/plain");

        assertThrows(Exception.class, () ->
                dataSourceImageService.updateImage(DATA_SOURCE_ID, file)
        );
    }

    @Test
    void updateImage_shouldThrow_whenDataSourceNotFound() {
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.empty());
        MultipartFile file = mock(MultipartFile.class);

        assertThrows(
                DataSourceNotFoundException.class,
                () -> dataSourceImageService.updateImage(DATA_SOURCE_ID, file)
        );
    }

    @Test
    void deleteImage_shouldDeleteImage_whenFound() throws Exception {
        var dataSource = mock(DataSource.class);
        Image image = mock(Image.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        when(dataSource.image()).thenReturn(image);

        dataSourceImageService.deleteImage(DATA_SOURCE_ID);

        verify(imageRepository).delete(image);
        verify(dataSource).setImage(null);
        verify(dataSourceRepository).save(dataSource);
    }

    @Test
    void deleteImage_shouldThrow_whenImageNotFound() {
        var dataSource = mock(DataSource.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        when(dataSource.image()).thenReturn(null);

        assertThrows(
                ImageNotFoundException.class,
                () -> dataSourceImageService.deleteImage(DATA_SOURCE_ID)
        );
    }

    @Test
    void deleteImage_shouldThrow_whenDataSourceNotFound() {
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.empty());

        assertThrows(
                DataSourceNotFoundException.class,
                () -> dataSourceImageService.deleteImage(DATA_SOURCE_ID)
        );
    }
}

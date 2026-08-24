// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.image.ImageFormatException;
import energy.eddie.aiida.errors.image.ImageNotFoundException;
import energy.eddie.aiida.errors.image.ImageReadException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.image.Image;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.ImageRepository;
import org.hibernate.Hibernate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DataSourceImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceImageService.class);

    private final ImageRepository imageRepository;
    private final DataSourceRepository dataSourceRepository;

    @Autowired
    public DataSourceImageService(
            ImageRepository imageRepository,
            DataSourceRepository dataSourceRepository
    ) {
        this.imageRepository = imageRepository;
        this.dataSourceRepository = dataSourceRepository;
    }

    @Transactional(readOnly = true)
    public Image imageByDataSourceId(UUID dataSourceId)
            throws ImageNotFoundException, DataSourceNotFoundException {
        var dataSource = dataSourceByIdOrThrow(dataSourceId);
        var image = dataSource.image();

        if (image == null) {
            throw new ImageNotFoundException(dataSource);
        }

        Hibernate.initialize(image);

        return image;
    }

    @Transactional(rollbackFor = {ImageReadException.class, ImageFormatException.class, DataSourceNotFoundException.class})
    public void updateImage(
            UUID dataSourceId,
            MultipartFile file
    ) throws ImageReadException, ImageFormatException, DataSourceNotFoundException {
        LOGGER.info("Updating image for data source with ID: {}", dataSourceId);

        var dataSource = dataSourceByIdOrThrow(dataSourceId);
        var newImage = imageFromMultipartFile(file);
        var image = dataSource.image();

        if (image != null) {
            imageRepository.delete(image);
        }

        dataSource.setImage(newImage);

        imageRepository.save(newImage);
        dataSourceRepository.save(dataSource);
        LOGGER.info("Image updated successfully for data source with ID: {}", dataSourceId);
    }

    @Transactional(rollbackFor = {DataSourceNotFoundException.class, ImageNotFoundException.class})
    public void deleteImage(UUID dataSourceId) throws ImageNotFoundException, DataSourceNotFoundException {
        LOGGER.info("Deleting image for data source with ID: {}", dataSourceId);

        var dataSource = dataSourceByIdOrThrow(dataSourceId);
        var image = dataSource.image();

        if (image == null) {
            throw new ImageNotFoundException(dataSource);
        }

        imageRepository.delete(image);
        dataSource.setImage(null);
        dataSourceRepository.save(dataSource);
        LOGGER.info("Image deleted successfully for data source with ID: {}", dataSourceId);
    }

    private DataSource dataSourceByIdOrThrow(UUID dataSourceId) throws DataSourceNotFoundException {
        return dataSourceRepository.findById(dataSourceId)
                                   .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId));
    }


    @SuppressWarnings("NullAway") // contentType is nullable, but it is checked before passed to any non-null method
    private Image imageFromMultipartFile(MultipartFile file) throws ImageReadException, ImageFormatException {
        var name = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
        var contentType = file.getContentType();

        if (!isValidContentType(contentType)) {
            throw new ImageFormatException(name);
        }

        try {
            return new Image(file.getBytes(), contentType);
        } catch (IOException e) {
            throw new ImageReadException(name, e);
        }
    }

    private boolean isValidContentType(@Nullable String contentType) {
        return contentType != null && (
                contentType.equals(MediaType.IMAGE_JPEG_VALUE) ||
                contentType.equals(MediaType.IMAGE_PNG_VALUE) ||
                contentType.equals("image/svg+xml")
        );
    }
}

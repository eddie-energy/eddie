package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.image.ImageFormatException;
import energy.eddie.aiida.errors.image.ImageNotFoundException;
import energy.eddie.aiida.errors.image.ImageReadException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.image.Image;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DataSourceImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceImageService.class);

    private final ImageRepository imageRepository;
    private final DataSourceRepository dataSourceRepository;

    @Autowired
    public DataSourceImageService(
            ImageRepository imageRepository,
            DataSourceRepository dataSourceRepository) {
        this.imageRepository = imageRepository;
        this.dataSourceRepository = dataSourceRepository;
    }

    public Image imageByDataSource(DataSource dataSource) throws ImageNotFoundException {
        var image = dataSource.image();

        if (image == null) {
            throw new ImageNotFoundException(dataSource);
        }

        return image;
    }

    public void updateImage(
            DataSource dataSource,
            MultipartFile file
    ) throws ImageReadException, ImageFormatException {
        LOGGER.info("Updating image for data source with ID: {}", dataSource.id());

        var newImage = imageFromMultipartFile(file);
        var image = dataSource.image();

        if (image != null) {
            imageRepository.delete(image);
        }

        dataSource.setImage(newImage);

        imageRepository.save(newImage);
        dataSourceRepository.save(dataSource);
        LOGGER.info("Image updated successfully for data source with ID: {}", dataSource.id());
    }

    public void deleteImage(DataSource dataSource) throws ImageNotFoundException {
        LOGGER.info("Deleting image for data source with ID: {}", dataSource.id());

        var image = dataSource.image();

        if (image == null) {
            throw new ImageNotFoundException(dataSource);
        }

        imageRepository.delete(image);
        dataSource.setImage(null);
        dataSourceRepository.save(dataSource);
        LOGGER.info("Image deleted successfully for data source with ID: {}", dataSource);
    }


    private Image imageFromMultipartFile(MultipartFile file) throws ImageReadException, ImageFormatException {
        try {
            var contentType = file.getContentType();

            if (!isValidContentType(contentType)) {
                throw new ImageFormatException(file.getOriginalFilename());
            }

            return new Image(file.getBytes(), contentType);
        } catch (IOException e) {
            throw new ImageReadException(file.getOriginalFilename());
        }
    }

    private boolean isValidContentType(String contentType) {
        return contentType != null && (
                contentType.equals(MediaType.IMAGE_JPEG_VALUE) ||
                contentType.equals(MediaType.IMAGE_PNG_VALUE) ||
                contentType.equals("image/svg+xml")
        );
    }
}

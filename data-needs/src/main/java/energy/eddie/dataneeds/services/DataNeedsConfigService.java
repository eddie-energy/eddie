package energy.eddie.dataneeds.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.dataneeds.exceptions.DataNeedAlreadyExistsException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjectionRecord;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * DataNeedService that reads data needs from a JSON file. It only stores the data needs in memory.
 */
@Service
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "CONFIG")
public class DataNeedsConfigService implements DataNeedsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedsConfigService.class);
    private final Map<String, DataNeed> dataNeeds = new ConcurrentHashMap<>();

    public DataNeedsConfigService(
            @Value("${eddie.data-needs-config.file}") String dataNeedsFilePath,
            ObjectMapper mapper,
            ApplicationContext context
    ) throws DataNeedAlreadyExistsException, IOException {
        // if declared as constructor dependency, validator.validate(dataNeed) fails because of an unresolved
        // dependency, but getting the validator directly from the context somehow works?
        var validator = context.getBean("validator", LocalValidatorFactoryBean.class);

        readDataNeedsFromFile(dataNeedsFilePath, mapper, validator);
    }

    private void readDataNeedsFromFile(
            String dataNeedsFilePath,
            ObjectMapper mapper,
            LocalValidatorFactoryBean validator
    ) throws IOException, DataNeedAlreadyExistsException, ValidationException {
        File file = new File(dataNeedsFilePath);
        TypeReference<List<DataNeed>> listOfDataNeedsTypeReference = new TypeReference<>() {};
        List<DataNeed> dataNeedsFromFile = mapper.readValue(file, listOfDataNeedsTypeReference);

        for (DataNeed dataNeed : dataNeedsFromFile) {
            Set<ConstraintViolation<DataNeed>> violations = validator.validate(dataNeed);

            String id;
            try {
                id = UUID.fromString(dataNeed.id()).toString();
            } catch (IllegalArgumentException ignored) {
                throw new ValidationException(
                        "Data need ID '%s' is not a valid UUID".formatted(dataNeed.id()));
            }


            if (!violations.isEmpty()) {
                String errorsString = Arrays.toString(
                        violations
                                .stream()
                                .map(violation -> "%s: %s".formatted(getFieldName(violation), violation.getMessage()))
                                .toArray(String[]::new)
                );

                throw new ValidationException(
                        "Failed to validate data need with ID '%s': %s".formatted(id, errorsString));
            }

            if (dataNeeds.containsKey(id))
                throw new DataNeedAlreadyExistsException(id);

            dataNeeds.put(id, dataNeed);
        }


        LOGGER.info("Loaded and validated {} data needs: {}", dataNeeds.size(),
                    dataNeedsFromFile.stream().map(DataNeed::name).toList());
    }

    private String getFieldName(ConstraintViolation<DataNeed> violation) {
        return StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                            .map(Path.Node::getName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining("."));
    }

    // use collect(Collectors.toUnmodifiableList()) instead of .toList() to get properly typed list
    @SuppressWarnings("java:S6204")
    @Override
    public List<DataNeedsNameAndIdProjection> getDataNeedIdsAndNames() {
        return dataNeeds.values()
                        .stream()
                        .map(DataNeedsNameAndIdProjectionRecord::new)
                        .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<DataNeed> findById(String id) {
        return Optional.ofNullable(dataNeeds.get(id));
    }

    @Override
    public DataNeed getById(String id) {
        return findById(id).orElseThrow(EntityNotFoundException::new);
    }
}

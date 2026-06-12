// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web;

import energy.eddie.outbound.rest.RestCimConfig;
import energy.eddie.outbound.rest.dto.CimCollection;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fallback converter for XML messages.
 * It can serialize and deserialize CIM messages and agnostic messages.
 * To achieve that it uses the {@link MarshallingHttpMessageConverter} for CIM messages and the {@link JacksonXmlHttpMessageConverter} for agnostic messages.
 * Furthermore, it splits each CIM document type into its own converter to prevent namespace pollution, where all namespaces registered in the {@link Jaxb2Marshaller} are added to the final XML document, even if they are not used in the document itself.
 */
@SuppressWarnings("NullableProblems")
public class FallbackXmlMessageConverter implements HttpMessageConverter<Object> {

    private final JacksonXmlHttpMessageConverter jacksonConverter;
    private final List<MarshallingHttpMessageConverter> converters = new ArrayList<>();

    public FallbackXmlMessageConverter(
            JacksonXmlHttpMessageConverter jacksonConverter
    ) {
        this.jacksonConverter = jacksonConverter;
        this.initJaxbMarshallers();
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return converters.stream().anyMatch(m -> m.canRead(clazz, mediaType))
               || jacksonConverter.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return converters.stream().anyMatch(m -> m.canWrite(clazz, mediaType))
               || jacksonConverter.canWrite(clazz, mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(
                new MediaType("application", "xml", StandardCharsets.UTF_8),
                new MediaType("text", "xml", StandardCharsets.UTF_8),
                new MediaType("application", "*+xml", StandardCharsets.UTF_8)
        );
    }

    @Override
    public Object read(
            Class<?> clazz,
            HttpInputMessage inputMessage
    ) throws IOException, HttpMessageNotReadableException {
        if (isJaxbAnnotated(clazz)) {
            for (var converter : converters) {
                if (converter.canRead(clazz, null)) {
                    return converter.read(clazz, inputMessage);
                }
            }
        }
        return jacksonConverter.read(clazz, inputMessage);
    }

    @Override
    public void write(Object o, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Class<?> clazz = o.getClass();
        if (isJaxbAnnotated(clazz)) {
            for (var converter : converters) {
                if (converter.canWrite(clazz, contentType)) {
                    converter.write(o, contentType, outputMessage);
                    return;
                }
            }
        }
        jacksonConverter.write(o, contentType, outputMessage);
    }

    private void initJaxbMarshallers() {
        for (var cimClass : RestCimConfig.cimClasses()) {
            var marshaller = new Jaxb2Marshaller();
            cimClass = new ArrayList<>(cimClass);
            cimClass.add(CimCollection.class);
            marshaller.setClassesToBeBound(cimClass.toArray(Class<?>[]::new));
            converters.add(new MarshallingHttpMessageConverter(marshaller));
        }
    }

    private boolean isJaxbAnnotated(Class<?> clazz) {
        return clazz.isAnnotationPresent(XmlRootElement.class);
    }
}

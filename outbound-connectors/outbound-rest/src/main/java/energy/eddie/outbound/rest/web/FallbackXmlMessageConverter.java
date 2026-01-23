// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SuppressWarnings("NullableProblems")
public class FallbackXmlMessageConverter implements HttpMessageConverter<Object> {

    private final MarshallingHttpMessageConverter jaxbConverter;
    private final JacksonXmlHttpMessageConverter jacksonConverter;

    public FallbackXmlMessageConverter(
            MarshallingHttpMessageConverter jaxbConverter,
            JacksonXmlHttpMessageConverter jacksonConverter
    ) {
        this.jaxbConverter = jaxbConverter;
        this.jacksonConverter = jacksonConverter;
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return jaxbConverter.canRead(clazz, mediaType) || jacksonConverter.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return jaxbConverter.canWrite(clazz, mediaType) || jacksonConverter.canWrite(clazz, mediaType);
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
            return jaxbConverter.read(clazz, inputMessage);
        } else {
            throw new HttpMessageNotReadableException(clazz + " cannot be read, as it is not required as input",
                                                      inputMessage);
        }
    }

    @Override
    public void write(Object o, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Class<?> clazz = o.getClass();
        if (isJaxbAnnotated(clazz)) {
            jaxbConverter.write(o, contentType, outputMessage);
        } else {
            jacksonConverter.write(o, contentType, outputMessage);
        }
    }

    private boolean isJaxbAnnotated(Class<?> clazz) {
        return clazz.isAnnotationPresent(XmlRootElement.class);
    }
}

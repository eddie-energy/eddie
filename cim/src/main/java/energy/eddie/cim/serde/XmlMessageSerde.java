// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import tools.jackson.databind.ObjectMapper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A {@link MessageSerde} implementation that produces CIM compliant XML.
 * Can also serialize and deserialize unknown types to and from XML.
 * Uses the JAXB for CIM documents and {@link ObjectMapper} as fallback.
 */
public class XmlMessageSerde implements MessageSerde {
    private static final Set<Class<?>> CIM_CLASSES = Set.of(
            // CIM v0.82
            PermissionEnvelope.class,
            ValidatedHistoricalDataEnvelope.class,
            AccountingPointEnvelope.class,
            // CIM v0.91.08
            RTREnvelope.class,
            // CIM v1.04
            VHDEnvelope.class,
            RTDEnvelope.class,
            // CIM v1.12
            energy.eddie.cim.v1_12.rtd.RTDEnvelope.class,
            AcknowledgementEnvelope.class,
            ESRDMDEnvelope.class,
            RECMMOEEnvelope.class,
            RequestPermissionEnvelope.class
    );
    private final Map<Class<?>, Marshaller> marshallers = new HashMap<>();
    private final Map<Class<?>, Unmarshaller> unmarshallers = new HashMap<>();
    private final ObjectMapper objectMapper;

    public XmlMessageSerde() throws SerdeInitializationException {
        objectMapper = ObjectMapperCreator.create(SerializationFormat.XML);
        initJAXB();
    }

    @Override
    public synchronized byte[] serialize(Object message) throws SerializationException {
        try {
            if (marshallers.containsKey(message.getClass())) {
                return serializeCimMessage(message, marshallers.get(message.getClass()));
            } else {
                return objectMapper.writeValueAsBytes(message);
            }
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] message, Class<T> messageType) throws DeserializationException {
        try {
            for (var entry : unmarshallers.entrySet()) {
                if (messageType.isAssignableFrom(entry.getKey())) {
                    return deserializeCimMessage(message, messageType, entry.getValue());
                }
            }
            return objectMapper.readValue(message, messageType);
        } catch (Exception e) {
            throw new DeserializationException(e);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public <T> List<T> deserializeList(byte[] message, Class<T> elementType) throws DeserializationException {
        try {
            var unmarshaller = findUnmarshaller(elementType);
            if (unmarshaller.isEmpty()) {
                return objectMapper.readValue(message,
                                              objectMapper.getTypeFactory()
                                                          .constructCollectionType(List.class, elementType));
            }
            var factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            var reader = factory.createXMLStreamReader(
                    new StringReader(new String(message, StandardCharsets.UTF_8))
            );

            // Skip to the root START_ELEMENT (container, name ignored)
            while (reader.hasNext() && reader.next() != XMLStreamConstants.START_ELEMENT) ;

            List<T> result = new ArrayList<>();
            while (reader.hasNext()) {
                var event = reader.next();
                if (event != XMLStreamConstants.START_ELEMENT) {
                    continue;
                }
                var res = deserializeCimMessage(reader, elementType, unmarshaller.get());
                result.add(res);
            }
            return result;
        } catch (Exception e) {
            throw new DeserializationException(e);
        }
    }

    private <T> Optional<Unmarshaller> findUnmarshaller(Class<T> elementType) {
        for (var entry : unmarshallers.entrySet()) {
            if (elementType.isAssignableFrom(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private void initJAXB() throws SerdeInitializationException {
        try {
            for (var cimClass : CIM_CLASSES) {
                var ctx = JAXBContext.newInstance(cimClass);
                var marshaller = ctx.createMarshaller();
                var unmarshaller = ctx.createUnmarshaller();
                marshallers.put(cimClass, marshaller);
                unmarshallers.put(cimClass, unmarshaller);
            }
        } catch (JAXBException e) {
            throw new SerdeInitializationException(e);
        }
    }

    private static byte[] serializeCimMessage(Object message, Marshaller marshaller) throws JAXBException {
        var os = new ByteArrayOutputStream();
        marshaller.marshal(message, os);
        return os.toByteArray();
    }

    private static <T> T deserializeCimMessage(byte[] message, Class<T> messageType, Unmarshaller unmarshaller)
            throws JAXBException, XMLStreamException {
        XMLInputFactory factory;
        factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        var reader = factory.createXMLStreamReader(
                new StringReader(new String(message, StandardCharsets.UTF_8))
        );
        return deserializeCimMessage(reader, messageType, unmarshaller);
    }

    private static <T> T deserializeCimMessage(XMLStreamReader message, Class<T> messageType, Unmarshaller unmarshaller)
            throws JAXBException {
        return unmarshaller.unmarshal(message, messageType).getValue();
    }
}

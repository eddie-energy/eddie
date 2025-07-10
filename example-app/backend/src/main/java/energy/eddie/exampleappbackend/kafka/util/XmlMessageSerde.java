package energy.eddie.exampleappbackend.kafka.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.cim.v0_91_08.VHDEnvelope;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A {@link MessageSerde} implementation that produces CIM compliant XML.
 * Can also serialize and deserialize unknown types to and from XML.
 * Uses the JAXB for CIM documents and {@link ObjectMapper} as fallback.
 */
public class XmlMessageSerde implements MessageSerde {
    private static final List<Class<?>> CIM_TYPES = List.of(
            PermissionEnvelope.class,
            ValidatedHistoricalDataEnvelope.class,
            AccountingPointEnvelope.class,
            VHDEnvelope.class
    );
    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;
    private final ObjectMapper objectMapper;

    public XmlMessageSerde() throws SerdeInitializationException {
        try {
            var ctx = JAXBContext.newInstance(
                    PermissionEnvelope.class,
                    AccountingPointEnvelope.class,
                    ValidatedHistoricalDataEnvelope.class,
                    RTREnvelope.class,
                    VHDEnvelope.class
            );
            marshaller = ctx.createMarshaller();
            unmarshaller = ctx.createUnmarshaller();
        } catch (JAXBException e) {
            throw new SerdeInitializationException(e);
        }
        objectMapper = ObjectMapperCreator.create(Format.XML);
    }

    @Override
    public byte[] serialize(Object message) throws SerializationException {
        try {
            return switch (message) {
                case ValidatedHistoricalDataEnvelope ignored -> serializeCimMessage(message);
                case PermissionEnvelope ignored -> serializeCimMessage(message);
                case AccountingPointEnvelope ignored -> serializeCimMessage(message);
                case RTREnvelope ignored -> serializeCimMessage(message);
                case VHDEnvelope ignored -> serializeCimMessage(message);
                default -> objectMapper.writeValueAsBytes(message);
            };
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] message, Class<T> messageType) throws DeserializationException {
        try {
            return isCimType(messageType)
                    ? deserializeCimMessage(message, messageType)
                    : objectMapper.readValue(message, messageType);
        } catch (Exception e) {
            throw new DeserializationException(e);
        }
    }

    private boolean isCimType(Class<?> type) {
        return CIM_TYPES.stream().anyMatch(type::isAssignableFrom);
    }

    private byte[] serializeCimMessage(Object message) throws JAXBException {
        var os = new ByteArrayOutputStream();
        marshaller.marshal(message, os);
        return os.toByteArray();
    }

    private <T> T deserializeCimMessage(byte[] message, Class<T> messageType) throws JAXBException, XMLStreamException {
        XMLInputFactory factory;
        factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        var reader = factory.createXMLStreamReader(
                new StringReader(new String(message, StandardCharsets.UTF_8))
        );
        return unmarshaller.unmarshal(reader, messageType).getValue();
    }
}

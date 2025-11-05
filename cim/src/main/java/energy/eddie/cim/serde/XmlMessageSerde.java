package energy.eddie.cim.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

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
            RTDEnvelope.class
    );
    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;
    private final ObjectMapper objectMapper;

    public XmlMessageSerde() throws SerdeInitializationException {
        try {
            var ctx = JAXBContext.newInstance(CIM_CLASSES.toArray(new Class<?>[0]));
            marshaller = ctx.createMarshaller();
            unmarshaller = ctx.createUnmarshaller();
        } catch (JAXBException e) {
            throw new SerdeInitializationException(e);
        }
        objectMapper = ObjectMapperCreator.create(SerializationFormat.XML);
    }

    @Override
    public synchronized byte[] serialize(Object message) throws SerializationException {
        try {
            if (CIM_CLASSES.contains(message.getClass())) {
                return serializeCimMessage(message);
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
            return isCimType(messageType)
                    ? deserializeCimMessage(message, messageType)
                    : objectMapper.readValue(message, messageType);
        } catch (Exception e) {
            throw new DeserializationException(e);
        }
    }

    private boolean isCimType(Class<?> type) {
        return CIM_CLASSES.stream().anyMatch(type::isAssignableFrom);
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

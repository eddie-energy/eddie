# Client Libraries

The common information model is a comprehensive model, making generating Java classes from the XSD files complicated.
[eddie-energy/eddie](https://github.com/eddie-energy/eddie) provides a maven artifact containing the relevant CIM classes.
If you already have access to the EDDIE repository, follow this [guide](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages).
If you do not have access to the EDDIE repository, contact the developers to gain access.

> [!INFO]
> Find the package with the CIM classes in the EDDIE repository [here](https://github.com/eddie-energy/eddie/packages/2495238).

## Getting started

The library includes many classes, but the most important ones to get started are:

- `energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope` for [validated historical date market documents](./validated-historical-data-market-documents.md)
- `energy.eddie.cim.v0_1_12.rtd.RTDEnvelope` for [near real-time data market documents](./near-real-time-data-documents.md)
- `energy.eddie.cim.v0_82.ap.AccountingPointEnvelope` for [accounting point date market documents](./accounting-point-data-market-documents.md)
- `energy.eddie.cim.v0_82.pmd.PermissionEnvelope` for [permission market documents](./permission-market-documents.md) and [termination documents](./permission-market-documents.md#termination-documents)
- `energy.eddie.cim.v0_91_08.RTREnvelope` for [redistribution transaction request documents](./redistribution-transaction-request-documents.md)

These classes can be used to parse incoming messages or to create messages that should be sent to the EDDIE framework.
The following example shows how the classes can be used to deserialize data from a string, which can then be used in Kafka or AMQP deserializers.
The example requires the [Jakarta XML bind API](https://mvnrepository.com/artifact/jakarta.xml.bind/jakarta.xml.bind-api) and a runtime, for example, [glassfish](https://mvnrepository.com/artifact/org.glassfish.jaxb/jaxb-runtime).

```kotlin
implementation("energy.eddie:cim:1.0.0")
```

```java
// All CIM documents:
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
// -----

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

class CimDeserializer {
    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;

    public XmlMessageSerde() throws JAXBException {
        var ctx = JAXBContext.newInstance(
                PermissionEnvelope.class,
                AccountingPointEnvelope.class,
                ValidatedHistoricalDataEnvelope.class,
                RTREnvelope.class
        );
        marshaller = ctx.createMarshaller();
        unmarshaller = ctx.createUnmarshaller();
    }

    private PermissionEnvelope deserializePermissionEnvelope(byte[] message) throws JAXBException, XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        var reader = factory.createXMLStreamReader(new StringReader(new String(message, StandardCharsets.UTF_8)));
        return unmarshaller.unmarshal(reader, PermissionEnvelope.class).getValue();
    }
}
```

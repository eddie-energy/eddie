# Client Libraries

The Common Information Model (CIM) is a comprehensive and complex model. Generating Java classes directly from the XSD files can therefore be challenging.
The repository [eddie-energy/eddie](https://github.com/eddie-energy/eddie) provides a Maven artifact that already contains the relevant CIM classes.

If you already have access to the EDDIE repository, follow this [guide](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages).
If you do not have access to the EDDIE repository, contact the developers to gain access.

## Getting started

The library contains a large number of classes. The following envelope classes are the most important entry points:

- `energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope` for [validated historical date market documents](./validated-historical-data-market-documents.md)
- `energy.eddie.cim.v0_1_12.rtd.RTDEnvelope` for [near real-time data market documents](./near-real-time-data-documents.md)
- `energy.eddie.cim.v0_82.ap.AccountingPointEnvelope` for [accounting point date market documents](./accounting-point-data-market-documents.md)
- `energy.eddie.cim.v0_82.pmd.PermissionEnvelope` for [permission market documents](./permission-market-documents.md) and [termination documents](./permission-market-documents.md#termination-documents)
- `energy.eddie.cim.v0_91_08.RTREnvelope` for [redistribution transaction request documents](./redistribution-transaction-request-documents.md)

These classes can be used to:

- Parse incoming XML messages
- Create messages that are sent to the EDDIE framework

The following example demonstrates how to deserialize an XML message from a string. This approach can be used in Kafka or AMQP deserializers.

The example requires:

- [Jakarta XML Bind API](https://mvnrepository.com/artifact/jakarta.xml.bind/jakarta.xml.bind-api)
- A JAXB runtime implementation, e.g. [Glassfish JAXB Runtime](https://mvnrepository.com/artifact/org.glassfish.jaxb/jaxb-runtime).

### Versions

The CIM library itself is versioned. The changelog can be found in the GitHub repository here: https://github.com/eddie-energy/eddie/blob/main/cim/CHANGELOG.md

### Importing the library

> [!INFO]
> You can find the CIM package in the EDDIE repository here: https://github.com/eddie-energy/eddie/packages/2495238.

::: code-group

```xml [Maven (XML)]
<dependency>
    <groupId>energy.eddie</groupId>
    <artifactId>cim</artifactId>
    <version>1.0.0</version> <!-- Use the desired version from GitHub Packages -->
</dependency>
```

```kotlin [Gradle (Kotlin)]
// Use the desired version from GitHub Packages
implementation("energy.eddie:cim:1.0.0")
```

```kotlin [Gradle (Groovy)]
// Use the desired version from GitHub Packages
implementation 'energy.eddie:cim:1.0.0'
```

:::

### Example code

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

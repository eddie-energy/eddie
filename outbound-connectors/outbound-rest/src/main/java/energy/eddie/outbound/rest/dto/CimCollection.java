package energy.eddie.outbound.rest.dto;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

// Used by Jaxb2 Marshaller
@SuppressWarnings("unused")
@XmlRootElement(name = "CimCollection")
public class CimCollection<T> {
    @Nullable
    private List<T> documents;

    public CimCollection(@Nullable List<T> documents) {this.documents = documents;}

    public CimCollection() {
        documents = null;
    }

    @Nullable
    @XmlElement(name = "Document")
    public List<T> getDocuments() {
        return documents;
    }

    public void setDocuments(@Nullable List<T> documents) {
        this.documents = documents;
    }
}

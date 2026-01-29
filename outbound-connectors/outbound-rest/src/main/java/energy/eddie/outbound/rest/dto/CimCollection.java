// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

// Used by Jaxb2 Marshaller
@SuppressWarnings("unused")
@XmlRootElement(name = "Collection")
public class CimCollection<T> {
    @Nullable
    private List<T> documents;

    public CimCollection(@Nullable List<T> documents) {this.documents = documents;}

    public CimCollection() {
        documents = null;
    }

    @Nullable
    @XmlAnyElement(lax = true)
    @JsonValue
    public List<T> getDocuments() {
        return documents;
    }

    public void setDocuments(@Nullable List<T> documents) {
        this.documents = documents;
    }
}

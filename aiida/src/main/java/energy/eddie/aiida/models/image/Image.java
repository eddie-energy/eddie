// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.image;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@SuppressWarnings("NullAway.Init")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;
    protected Instant createdAt;
    @Column(name = "data", columnDefinition = "BYTEA")
    protected byte[] data;
    protected String contentType;

    protected Image() {
        // Default constructor for JPA
    }

    public Image(byte[] data, String contentType) {
        this.createdAt = Instant.now();
        this.data = data;
        this.contentType = contentType;
    }

    public UUID id() {
        return id;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public byte[] data() {
        return data;
    }

    public String contentType() {
        return contentType;
    }
}

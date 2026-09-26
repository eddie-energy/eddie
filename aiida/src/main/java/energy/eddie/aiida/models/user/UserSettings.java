// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Per-user settings of an AIIDA user. A row is only present once a user configured settings.
 */
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    @JsonProperty
    @Schema(description = "ID of the user these settings belong to.", example = "092bf5cb-8571-4313-9429-8caf5e679f6e")
    private UUID userId;

    @Nullable
    @Column(name = "contact_email")
    @JsonProperty
    @Schema(description = "Email address used for connection limit notifications. Null disables notifications.", example = "user@example.com")
    private String contactEmail;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected UserSettings() {
    }

    public UserSettings(UUID userId, @Nullable String contactEmail) {
        this.userId = userId;
        this.contactEmail = contactEmail;
    }

    public UUID userId() {
        return userId;
    }

    @Nullable
    public String contactEmail() {
        return contactEmail;
    }

    public void setContactEmail(@Nullable String contactEmail) {
        this.contactEmail = contactEmail;
    }
}

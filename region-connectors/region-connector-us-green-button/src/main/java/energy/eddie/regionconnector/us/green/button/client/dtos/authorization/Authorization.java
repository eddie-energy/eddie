package energy.eddie.regionconnector.us.green.button.client.dtos.authorization;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Export;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Exports;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public record Authorization(
        @JsonProperty(value = "uid", required = true)
        String uid,
        @JsonProperty(value = "created", required = true)
        ZonedDateTime created,
        @JsonProperty(value = "customer_email", required = true)
        String customerEmail,
        @JsonProperty(value = "customer_signature")
        @Nullable
        CustomerSignature customerSignature,
        @JsonProperty(value = "declined")
        @Nullable
        ZonedDateTime declined,
        @JsonProperty(value = "is_declined", required = true)
        boolean isDeclined,
        @JsonProperty(value = "expires", required = true)
        String expires,
        @JsonProperty(value = "is_expired", required = true)
        boolean isExpired,
        @JsonProperty(value = "exports", required = true)
        Exports exports,
        @JsonProperty(value = "exports_list")
        @Nullable
        List<Export> exportsList,
        @JsonProperty(value = "form_uid")
        @Nullable
        String formUid,
        @JsonProperty(value = "template_uid")
        @Nullable
        String templateUid,
        @JsonProperty(value = "referrals")
        @Nullable
        List<String> referrals,
        @JsonProperty(value = "is_archived", required = true)
        boolean isArchived,
        @JsonProperty(value = "is_test", required = true)
        boolean isTest,
        @JsonProperty(value = "notes", required = true)
        List<AuthorizationNotes> notes,
        @JsonProperty(value = "nickname")
        @Nullable
        String nickname,
        @JsonProperty(value = "revoked")
        @Nullable
        ZonedDateTime revoked,
        @JsonProperty(value = "is_revoked", required = true)
        boolean isRevoked,
        @JsonProperty(value = "scope", required = true)
        Scope scope,
        @JsonProperty(value = "status", required = true)
        String status,
        @JsonProperty(value = "status_message", required = true)
        String statusMessage,
        @JsonProperty(value = "status_ts", required = true)
        ZonedDateTime statusTs,
        @JsonProperty(value = "user_email", required = true)
        String userEmail,
        @JsonProperty(value = "user_uid", required = true)
        String userUid,
        @JsonProperty(value = "user_status", required = true)
        String userStatus,
        @JsonProperty(value = "utility", required = true)
        String utility
) {
}

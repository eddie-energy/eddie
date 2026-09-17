// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.e2etests.aiida;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.e2etests.PlaywrightOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static energy.eddie.e2etests.PlaywrightOptions.*;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UsePlaywright(PlaywrightOptions.class)
class AiidaUiTests {
    Page page;
    BrowserContext context;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup(Page page, BrowserContext context) {
        this.page = page;
        this.context = context;

        page.navigate(AIIDA_URL);

        page.getByLabel("Username").fill("aiida");
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("aiida");
        page.getByRole(AriaRole.BUTTON).getByText("Sign In").click();
    }

    @Test
    void dataSourceAndPermissionFlow(Page page) {
        var dataSource = "E2E Simulation Data Source";
        var dataNeed = "FUTURE_NEAR_REALTIME_DATA_OUTBOUND";

        // Click "Data Sources" tab
        page.getByRole(AriaRole.LINK).getByText("Data Sources").click();
        // Click "Add Data Source" button
        page.getByRole(AriaRole.BUTTON).getByText("Add Data Source").click();
        // Fill fields
        page.getByLabel("Name").fill(dataSource);
        page.getByRole(AriaRole.LISTBOX).getByText("Asset Type").click();
        page.getByRole(AriaRole.OPTION).getByText("CONNECTION-AGREEMENT-POINT").click();
        page.getByRole(AriaRole.LISTBOX).getByText("Data Source Type").click();
        page.getByRole(AriaRole.OPTION).getByText("Simulation").click();
        page.getByRole(AriaRole.LISTBOX).getByText("Country").click();
        page.getByRole(AriaRole.OPTION).getByText("Austria").click();
        page.getByLabel("Polling Interval").fill("120");
        // Choose icon
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Meter")).click();
        // Upload image
        page.waitForFileChooser(() -> page.getByText("Browse files").click())
            .setFiles(Paths.get("src/test/resources/datasource.png"));
        // Click "Add"
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add").setExact(true)).click();
        // Visible data source with name in list
        var dataSourceCard = page.getByRole(AriaRole.ARTICLE)
                                 .filter(new Locator.FilterOptions().setHasText(dataSource))
                                 .last();
        assertThat(dataSourceCard).isVisible();
        // Visible image
        assertThat(dataSourceCard.getByAltText("image for data source")).isVisible();

        // Create permission request through the EDDIE button
        var aiidaCode = aiidaCodeForDataNeed(dataNeed);

        // Accept permission request through the AIIDA UI
        var permissionId = acceptOutboundPermissionRequest(aiidaCode, dataSource);

        // Select "FUTURE_NEAR_REALTIME_DATA" in list
        var permission = selectPermission(dataNeed, PermissionTab.OUTBOUND, PermissionStatus.ACTIVE);
        // Check if permission has the right data need and permission ID
        assertThat(permission).containsText(dataNeed);
        assertThat(permission).containsText(permissionId);
        // Check if status is correct
        assertThat(permission).containsText("Streaming Data");

        // Revoke permission
        revokePermission(permission);

        // Check if permission is in the complete tab
        permission = selectPermission(dataNeed, PermissionTab.OUTBOUND, PermissionStatus.COMPLETE);
        // Check if permission has the right data need and permission ID
        assertThat(permission).containsText(dataNeed);
        assertThat(permission).containsText(permissionId);
        // Check if permission has revoked status
        assertThat(permission).containsText("Revoked");

        // Delete data source
        deleteDataSource(dataSource);
        assertThat(dataSourceCard).isHidden();
    }

    @Test
    void inboundMessageForwardingFlow(APIRequestContext request) throws IOException {
        var inboundDataNeed = "FUTURE_MIN_MAX_ENVELOPE_INBOUND";
        var inboundDataNeedId = "f7698978-b9fe-40c8-aebe-c997f7f58f2f";
        var inboundPermissionDisplayName = "Control signal for my living room";
        var outboundDataNeed = "Forward inbound opaque and min-max envelopes";
        var outboundDataNeedId = "5de2a77d-1dd4-458d-b700-a84884dd04c6";

        var inboundAiidaCode = aiidaCodeForDataNeed(inboundDataNeed);
        var inboundPermissionId = acceptInboundPermissionRequest(inboundAiidaCode, inboundPermissionDisplayName);

        var outboundAiidaCode = aiidaCodeForDataNeed(outboundDataNeed);
        var outboundPermissionId = acceptOutboundPermissionRequest(outboundAiidaCode, inboundPermissionDisplayName);

        // Wait for connector to start
        page.waitForTimeout(1000);

        // Send opaque envelope via EDDIE
        var opaqueEnvelope = Map.of(
                "connectionId", "1",
                "permissionId", inboundPermissionId,
                "dataNeedId", inboundDataNeedId,
                "regionConnectorId", "aiida",
                "payload", "E2E"
        );
        var response = request.post(REST_URL + "/agnostic/opaque-envelope", RequestOptions.create().setData(opaqueEnvelope));
        assertThat(response).isOK();

        // Wait for message to arrive
        page.waitForTimeout(1000);

        // Check if the envelope arrived in inbound and outbound
        var permission = selectPermission(outboundDataNeed, PermissionTab.OUTBOUND, PermissionStatus.ACTIVE);
        var download = page.waitForDownload(() -> permission.getByText("Download Latest Message").click());

        var latestMessage = Files.readString(download.path());
        var root = mapper.readTree(latestMessage);
        var messages = root.path("messages").get(0);
        var rawMessage = messages.path("message").asString();
        var message = mapper.readTree(rawMessage);

        // Confirm relay to outbound permission
        assertEquals(outboundPermissionId, message.path("permissionId").asString());
        assertEquals(outboundDataNeedId, message.path("dataNeedId").asString());

        // Try revoke inbound while blocked
        revokeInboundPermission(inboundPermissionDisplayName);
        expectAlert("Cannot revoke inbound permission %s because it is still used by outbound permissions: %s"
                            .formatted(inboundPermissionId, outboundPermissionId));
        // Revoke outbound
        revokeOutboundPermission(outboundDataNeed);
        expectAlert("The permission for this service was revoked.");

        // Revoke inbound
        revokeInboundPermission(inboundPermissionDisplayName);
        expectAlert("The permission for this service was revoked.");
    }

    @Test
    void connectionLimitFlow(APIRequestContext request) throws IOException {
        var dataNeed = "FUTURE_MIN_MAX_ENVELOPE_INBOUND";
        var meterId = "e2e-limit-meter";
        var dataNeedId = "f7698978-b9fe-40c8-aebe-c997f7f58f2f";
        var displayName = "E2E Limit Permission";
        var defaultMin = 10;
        var defaultMax = 20;
        var updatedDefaultMin = 1;
        var updatedDefaultMax = 10;
        var documentMin = 3;
        var documentMax = 8;
        var documentId = UUID.randomUUID().toString();
        var now = Instant.now();
        var documentIntervalStart = now.minusSeconds(60);
        var documentIntervalEnd = documentIntervalStart.plusSeconds(86_400);
        var queryStart = documentIntervalStart.minusSeconds(3600);
        var queryEnd = documentIntervalEnd.plusSeconds(3600);

        // Create permission with limit defaults
        var aiidaCode = aiidaCodeForDataNeed(dataNeed, meterId, defaultMin, defaultMax);
        var permissionId = acceptInboundPermissionRequest(aiidaCode, displayName);

        // Successful inbound permission shows meter ID and limit defaults
        var permission = selectPermission(displayName, PermissionTab.INBOUND, PermissionStatus.ACTIVE);
        // String concat with '+' for readability where the DOM would show the text content without a whitespace
        assertThat(permission).containsText("Meter ID" + meterId);
        assertThat(permission).containsText("Minimum load" + "%d kW".formatted(defaultMin));
        assertThat(permission).containsText("Maximum load" + "%d kW".formatted(defaultMax));

        // Initial default limits are returned by the effective connection limit API
        var initialLimits = getConnectionLimits(permissionId, queryStart, queryEnd);
        var initialLimitsJson = mapper.readTree(initialLimits);
        assertTrue(containsConnectionLimit(initialLimitsJson, null, defaultMin, defaultMax));

        // Send command to update limit defaults
        var limitCommand = new PermissionCommand.UpdateLimitDefaults("aiida",
                                                                     UUID.fromString(permissionId),
                                                                     BigDecimal.valueOf(updatedDefaultMin),
                                                                     BigDecimal.valueOf(updatedDefaultMax));
        var commandResponse = request.post(REST_URL + "/agnostic/permission-command",
                                           RequestOptions.create()
                                                         .setHeader("content-type", "application/json")
                                                         .setData(mapper.writeValueAsString(limitCommand)));
        assertThat(commandResponse).isOK();

        // Effective limits should include historic and updated limits
        waitForConnectionLimits(permissionId, queryStart, queryEnd, null, updatedDefaultMin, updatedDefaultMax, mapper);

        page.reload();
        permission = selectPermission(displayName, PermissionTab.INBOUND, PermissionStatus.ACTIVE);
        assertThat(permission).containsText("Minimum load" + "%d kW".formatted(updatedDefaultMin));
        assertThat(permission).containsText("Maximum load" + "%d kW".formatted(updatedDefaultMax));

        // Send min-max envelope
        var minMaxResponse = request.post(REST_URL + "/cim_1_12/min-max-envelope-md",
                                          RequestOptions.create()
                                                        .setHeader("content-type", "application/json")
                                                        .setData(minMaxEnvelope(permissionId,
                                                                                dataNeedId,
                                                                                meterId,
                                                                                documentId,
                                                                                now,
                                                                                documentIntervalStart,
                                                                                documentIntervalEnd,
                                                                                documentMin,
                                                                                documentMax)));
        assertThat(minMaxResponse).isOK();

        var effectiveLimits = waitForConnectionLimits(permissionId,
                                                      queryStart,
                                                      queryEnd,
                                                      documentId,
                                                      documentMin,
                                                      documentMax,
                                                      mapper);
        var effectiveLimitsJson = mapper.readTree(effectiveLimits);
        assertTrue(containsConnectionLimit(effectiveLimitsJson, null, updatedDefaultMin, updatedDefaultMax));
        assertTrue(containsConnectionLimit(effectiveLimitsJson, documentId, documentMin, documentMax));

        revokePermission(permission);
    }

    @Test
    void fcaPermissionFlow() {
        var dataSource = "E2E FCA Data Source";
        var otherDataSource = "E2E FCA Other Data Source";
        var inboundDataNeed = "FCA Inbound";
        var outboundDataNeed = "FCA Outbound";
        var meterId = "e2e-fca-meter";
        var otherMeterId = "e2e-other-meter";

        // Create inbound permission sending min-max envelopes
        var inboundAiidaCode = aiidaCodeForDataNeed(inboundDataNeed, meterId);
        var inboundPermissionId = acceptInboundPermissionRequest(inboundAiidaCode, inboundDataNeed);

        // Successful inbound permission shows FCA badge and meter ID
        var inboundPermission = selectPermission(inboundDataNeed, PermissionTab.INBOUND, PermissionStatus.ACTIVE);
        assertThat(inboundPermission).containsText(inboundPermissionId);
        assertThat(inboundPermission).containsText("FCA");
        assertThat(inboundPermission).containsText(meterId);

        // Attempting a second FCA inbound permission for the same meter ID must fail
        var duplicateAiidaCode = aiidaCodeForDataNeed(inboundDataNeed, meterId);
        addPermission(duplicateAiidaCode);
        var expectedError = "active inbound FCA permission for meter ID '%s'".formatted(meterId);
        assertThat(page.getByRole(AriaRole.DIALOG)).containsText(expectedError);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel").setExact(true)).click();
        expectAlert(expectedError);

        // Failed duplicate ends up as unable to fulfill in the complete tab
        var failedPermission = selectPermission(inboundDataNeed, PermissionTab.INBOUND, PermissionStatus.COMPLETE);
        assertThat(failedPermission).containsText("Unable to fulfill");

        // Create a data source matching the FCA meter ID, and one that doesn't match
        createSimulationDataSource(dataSource, meterId);
        createSimulationDataSource(otherDataSource, otherMeterId);

        // Accept an FCA outbound permission tied to the meter ID
        // The data source with a different meter ID is not shown
        // The matching data source with the same meter ID gets pre-selected
        var outboundAiidaCode = aiidaCodeForDataNeed(outboundDataNeed, meterId);
        var outboundPermissionId = acceptOutboundPermissionRequestWithPreselectedDataSource(outboundAiidaCode,
                                                                                            dataSource);

        // Successful outbound permission shows FCA badge and meter ID
        var outboundPermission = selectPermission(outboundDataNeed, PermissionTab.OUTBOUND, PermissionStatus.ACTIVE);
        assertThat(outboundPermission).containsText(outboundPermissionId);
        assertThat(outboundPermission).containsText("FCA");
        assertThat(outboundPermission).containsText(meterId);

        // Clean up
        revokeInboundPermission(inboundDataNeed);
        revokeOutboundPermission(outboundDataNeed);
        deleteDataSource(dataSource);
        deleteDataSource(otherDataSource);
    }

    @Test
    void logout(Page page) {
        page.getByText("Account").click();
        page.getByText("Logout").click();
        assertThat(page.getByText("Sign in to your account")).isVisible();
    }

    private String aiidaCodeForDataNeed(String dataNeed) {
        return aiidaCodeForDataNeed(dataNeed, null);
    }

    private String aiidaCodeForDataNeed(String dataNeed, String meterId) {
        return aiidaCodeForDataNeed(dataNeed, meterId, null, null);
    }

    private String aiidaCodeForDataNeed(String dataNeed, String meterId, Integer min, Integer max) {
        var eddie = context.newPage();
        eddie.navigate(EDDIE_URL + "/demo");

        eddie.getByLabel("Data need (required)").selectOption(dataNeed);
        if (meterId != null) {
            eddie.getByLabel("Meter ID").fill(meterId);
        }
        if (min != null) {
            eddie.getByLabel("Min Limit").fill(min.toString());
        }
        if (max != null) {
            eddie.getByLabel("Max Limit").fill(max.toString());
        }

        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).click();
        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue")).click();
        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        return eddie.getByLabel("AIIDA code").inputValue();
    }

    private void addPermission(String aiidaCode) {
        page.bringToFront();
        page.getByRole(AriaRole.LINK).getByText("Permissions").click();
        page.getByRole(AriaRole.BUTTON).getByText("Add Permission").click();
        page.getByPlaceholder("AIIDA Code").fill(aiidaCode);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add").setExact(true)).click();
    }

    private String acceptInboundPermissionRequest(String aiidaCode, String displayName) {
        addPermission(aiidaCode);

        var dialog = page.getByRole(AriaRole.DIALOG);
        var id = dialog.locator(":text('Permission ID') + dd").textContent();
        dialog.getByPlaceholder("Enter a display name for this permission").fill(displayName);
        dialog.getByRole(AriaRole.BUTTON).getByText("Accept").click();

        return id;
    }

    private String acceptOutboundPermissionRequest(String aiidaCode, String dataSource) {
        addPermission(aiidaCode);

        var dialog = page.getByRole(AriaRole.DIALOG);
        var id = dialog.locator(":text('Permission ID') + dd").textContent();

        dialog.getByRole(AriaRole.LISTBOX).click();
        dialog.getByRole(AriaRole.OPTION).getByText(dataSource).first().click();

        dialog.getByRole(AriaRole.BUTTON).getByText("Accept").click();

        return id;
    }

    private String acceptOutboundPermissionRequestWithPreselectedDataSource(String aiidaCode, String dataSource) {
        addPermission(aiidaCode);

        var dialog = page.getByRole(AriaRole.DIALOG);
        var id = dialog.locator(":text('Permission ID') + dd").textContent();
        var listbox = dialog.getByRole(AriaRole.LISTBOX);

        // The one data source that matches the meter ID is pre-selected
        assertThat(listbox).containsText(dataSource);

        // The data source with a different meter ID is not displayed
        listbox.click();
        assertThat(dialog.getByRole(AriaRole.OPTION)).hasCount(1);
        listbox.click();

        assertThat(dialog.getByRole(AriaRole.BUTTON).getByText("Accept")).isEnabled();
        dialog.getByRole(AriaRole.BUTTON).getByText("Accept").click();

        return id;
    }

    private void createSimulationDataSource(String name, String meterId) {
        page.getByRole(AriaRole.LINK).getByText("Data Sources").click();
        page.getByRole(AriaRole.BUTTON).getByText("Add Data Source").click();
        page.getByLabel("Name").fill(name);
        page.getByRole(AriaRole.LISTBOX).getByText("Asset Type").click();
        page.getByRole(AriaRole.OPTION).getByText("CONNECTION-AGREEMENT-POINT").click();
        page.getByRole(AriaRole.LISTBOX).getByText("Data Source Type").click();
        page.getByRole(AriaRole.OPTION).getByText("Simulation").click();
        page.getByRole(AriaRole.LISTBOX).getByText("Country").click();
        page.getByRole(AriaRole.OPTION).getByText("Austria").click();
        page.getByLabel("Polling Interval").fill("120");
        page.getByLabel("Physical Meter ID").fill(meterId);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Meter")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add").setExact(true)).click();
    }

    private void revokeInboundPermission(String name) {
        var permission = selectPermission(name, PermissionTab.INBOUND, PermissionStatus.ACTIVE);
        revokePermission(permission);
    }

    private void revokeOutboundPermission(String name) {
        var permission = selectPermission(name, PermissionTab.OUTBOUND, PermissionStatus.ACTIVE);
        revokePermission(permission);
    }

    private void revokePermission(Locator permission) {
        permission.getByRole(AriaRole.BUTTON).getByText("Revoke").click();
        page.getByRole(AriaRole.DIALOG).getByRole(AriaRole.BUTTON).getByText("Revoke").click();
    }

    private Locator selectPermission(String name, PermissionTab tab, PermissionStatus status) {
        page.getByRole(AriaRole.NAVIGATION).getByRole(AriaRole.LINK).getByText("Permissions").click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(tab.label)).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(status.label)).click();
        var heading = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(name)).first();
        heading.click();
        return page.getByRole(AriaRole.LISTITEM).filter(new Locator.FilterOptions().setHas(heading)).first();
    }

    private void deleteDataSource(String dataSource) {
        page.getByRole(AriaRole.NAVIGATION).getByRole(AriaRole.LINK).getByText("Data Sources").click();
        var card = page.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions().setHasText(dataSource)).last();
        card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete")).click();
        page.getByRole(AriaRole.DIALOG)
            .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete")).click();
    }

    private void expectAlert(String message) {
        assertThat(page.getByRole(AriaRole.ALERT).filter(new Locator.FilterOptions().setHasText(message))).isVisible();
    }

    private String getConnectionLimits(String permissionId, Instant from, Instant to) {
        var response = context.request()
                              .get(AIIDA_URL + "/connection-limits",
                                   RequestOptions.create()
                                                 .setHeader("Authorization", "Bearer " + fetchAccessToken())
                                                 .setQueryParam("permissionId", permissionId)
                                                 .setQueryParam("from", from.toString())
                                                 .setQueryParam("to", to.toString()));
        assertThat(response).isOK();
        return response.text();
    }

    private String waitForConnectionLimits(
            String permissionId,
            Instant from,
            Instant to,
            String documentId,
            int minLimitKw,
            int maxLimitKw,
            ObjectMapper mapper
    ) {
        var limits = "";
        for (var attempt = 0; attempt < 20; attempt++) {
            limits = getConnectionLimits(permissionId, from, to);
            if (containsConnectionLimit(mapper.readTree(limits), documentId, minLimitKw, maxLimitKw)) {
                return limits;
            }
            page.waitForTimeout(500);
        }
        assertTrue(containsConnectionLimit(mapper.readTree(limits), documentId, minLimitKw, maxLimitKw),
                   "Expected connection limits to contain document ID %s with min %s and max %s, but got: %s".formatted(
                           documentId,
                           minLimitKw,
                           maxLimitKw,
                           limits));
        return limits;
    }

    private boolean containsConnectionLimit(JsonNode limits, String documentId, int minLimitKw, int maxLimitKw) {
        for (var limit : limits) {
            var documentIdNode = limit.path("documentId");
            var documentIdMatches = documentId == null ? documentIdNode.isNull() : documentId.equals(documentIdNode.asString());
            var minLimitMatches = limit.path("minLimitKw").asInt() == minLimitKw;
            var maxLimitMatches = limit.path("maxLimitKw").asInt() == maxLimitKw;
            if (documentIdMatches && minLimitMatches && maxLimitMatches) {
                return true;
            }
        }
        return false;
    }

    private String minMaxEnvelope(
            String permissionId,
            String dataNeedId,
            String meterId,
            String documentId,
            Instant createdAt,
            Instant intervalStart,
            Instant intervalEnd,
            int minLimitKw,
            int maxLimitKw
    ) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream("min-max-envelope.json")) {
            var envelope = new String(requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
            return envelope.replace("{permissionId}", permissionId)
                           .replace("{dataNeedId}", dataNeedId)
                           .replace("{meterId}", meterId)
                           .replace("{documentId}", documentId)
                           .replace("{createdAt}", createdAt.toString())
                           .replace("{intervalStart}", intervalStart.toString())
                           .replace("{intervalEnd}", intervalEnd.toString())
                           .replace("{minLimitKw}", Integer.toString(minLimitKw))
                           .replace("{maxLimitKw}", Integer.toString(maxLimitKw));
        }
    }

    private String fetchAccessToken() {
        var keycloakUrl = page.evaluate("THYMELEAF_KEYCLOAK_URL").toString();
        var realm = page.evaluate("THYMELEAF_KEYCLOAK_REALM").toString();
        var clientId = page.evaluate("THYMELEAF_KEYCLOAK_CLIENT").toString();

        var response = context.request().post(
                keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                RequestOptions.create()
                              .setHeader("content-type", "application/x-www-form-urlencoded")
                              .setForm(FormData.create()
                                               .set("grant_type", "password")
                                               .set("client_id", clientId)
                                               .set("username", "aiida")
                                               .set("password", "aiida")));
        assertThat(response).isOK();
        return mapper.readTree(response.text()).path("access_token").asString();
    }

    private enum PermissionTab {
        OUTBOUND("Outbound Permissions"),
        INBOUND("Inbound Permissions");

        public final String label;

        PermissionTab(String label) {this.label = label;}
    }

    private enum PermissionStatus {
        ACTIVE("Active"),
        PENDING("Pending"),
        COMPLETE("Complete");

        public final String label;

        PermissionStatus(String label) {this.label = label;}
    }
}

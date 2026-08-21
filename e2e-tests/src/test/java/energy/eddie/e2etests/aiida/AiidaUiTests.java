// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.e2etests.aiida;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.RequestOptions;
import energy.eddie.e2etests.PlaywrightOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static energy.eddie.e2etests.PlaywrightOptions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@UsePlaywright(PlaywrightOptions.class)
class AiidaUiTests {
    Page page;
    BrowserContext context;

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
        var mapper = new ObjectMapper();

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
    void fcaPermissionFlow() {
        var dataSource = "E2E FCA Data Source";
        var otherDataSource = "E2E FCA Other Data Source";
        var fcaOutboundDataNeed = "FCA Outbound";
        var meterId = "e2e-meter";
        var otherMeterId = "e2e-meter-other";

        // Create a data source matching the FCA meter ID, and one that doesn't match
        createSimulationDataSource(dataSource, meterId);
        createSimulationDataSource(otherDataSource, otherMeterId);

        // Accept an FCA outbound permission tied to the meter ID
        // The data source with a different meter ID is not shown
        // The matching data source with the same meter ID gets pre-selected
        var aiidaCode = aiidaCodeForDataNeedWithMeterId(fcaOutboundDataNeed, meterId);
        var permissionId =
                acceptOutboundPermissionRequestWithPreselectedDataSource(aiidaCode, dataSource, otherDataSource);

        // Attempting a second FCA outbound permission for the same meter ID must fail
        var duplicateAiidaCode = aiidaCodeForDataNeedWithMeterId(fcaOutboundDataNeed, meterId);
        addPermission(duplicateAiidaCode);
        var expectedError = "active outbound FCA permission for meter ID '%s'".formatted(meterId);
        assertThat(page.getByRole(AriaRole.DIALOG)).containsText(expectedError);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel").setExact(true)).click();
        expectAlert(expectedError);

        // Failed duplicate ends up as unable to fulfill in the complete tab
        var failedPermission = selectPermission(fcaOutboundDataNeed, PermissionTab.OUTBOUND, PermissionStatus.COMPLETE);
        assertThat(failedPermission).containsText("Unable to fulfill");

        // Successful permission shows FCA badge and meter ID
        var permission = selectPermission(fcaOutboundDataNeed, PermissionTab.OUTBOUND, PermissionStatus.ACTIVE);
        assertThat(permission).containsText(permissionId);
        assertThat(permission).containsText("FCA");
        assertThat(permission).containsText(meterId);

        // Clean up
        revokePermission(permission);
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
        var eddie = context.newPage();
        eddie.navigate(EDDIE_URL + "/demo");
        eddie.getByLabel("Data need").selectOption(dataNeed);
        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).click();
        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue")).click();
        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        return eddie.getByLabel("AIIDA code").inputValue();
    }

    private String aiidaCodeForDataNeedWithMeterId(String dataNeed, String meterId) {
        var eddie = context.newPage();
        eddie.navigate(EDDIE_URL + "/demo");
        eddie.getByLabel("Data need").selectOption(dataNeed);
        eddie.getByLabel("Meter ID").fill(meterId);
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

    private String acceptOutboundPermissionRequestWithPreselectedDataSource(
            String aiidaCode, String expectedDataSource, String excludedDataSource) {
        addPermission(aiidaCode);

        var dialog = page.getByRole(AriaRole.DIALOG);
        var id = dialog.locator(":text('Permission ID') + dd").textContent();
        var listbox = dialog.getByRole(AriaRole.LISTBOX);

        // The one data source that matches the meter ID is pre-selected
        assertThat(listbox).containsText(expectedDataSource);

        // The data source with a different meter ID is not displayed
        listbox.click();
        assertThat(dialog.getByRole(AriaRole.OPTION).getByText(excludedDataSource)).hasCount(0);
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

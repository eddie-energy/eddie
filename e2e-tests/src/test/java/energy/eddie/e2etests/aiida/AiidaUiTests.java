package energy.eddie.e2etests.aiida;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.e2etests.PlaywrightOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static energy.eddie.e2etests.PlaywrightOptions.AIIDA_URL;
import static energy.eddie.e2etests.PlaywrightOptions.EDDIE_URL;

@UsePlaywright(PlaywrightOptions.class)
class AiidaUiTests {

    @BeforeEach
    void login(Page page) {
        page.navigate(AIIDA_URL);

        page.getByLabel("Username").fill("aiida");
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("aiida");
        page.getByRole(AriaRole.BUTTON).getByText("Sign In").click();
    }

    @Test
    void dataSourceAndPermissionFlow(Page page, BrowserContext context) {
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
        page.locator(".icon-button:has(svg:has-text('Heat'))").first().click();
        // Upload image
        page.waitForFileChooser(() -> page.getByText("Browse files").click())
            .setFiles(Paths.get("src/test/resources/datasource.png"));
        // Click "Add"
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add").setExact(true)).click();
        // Visible data source with name in list
        var dataSourceCard = page.getByRole(AriaRole.ARTICLE).filter(
                new Locator.FilterOptions().setHasText(dataSource));
        assertThat(dataSourceCard).isVisible();
        // Visible image
        assertThat(dataSourceCard.getByAltText("image for data source")).isVisible();
        // Visible icon
        assertThat(dataSourceCard.locator("svg:has-text('Heat')")).isVisible();

        // Create permission request through the EDDIE button
        var eddie = context.newPage();
        eddie.navigate(EDDIE_URL + "/demo");
        eddie.getByLabel("Data need").selectOption(dataNeed);
        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).click();
        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue")).click();
        eddie.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        var code = eddie.getByLabel("AIIDA code").inputValue();

        // Switch back to AIIDA page
        page.bringToFront();
        // Click "Permissions" tab
        page.getByRole(AriaRole.LINK).getByText("Permissions").click();
        // Click "Add Permission"
        page.getByRole(AriaRole.BUTTON).getByText("Add Permission").click();
        // Fill "AIIDA Code" -> How to generate?
        page.getByPlaceholder("AIIDA Code").fill(code);
        // Click "Add" button
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add").setExact(true)).click();
        // Save permission ID for later reference
        var permissionId = page.locator(":text('Permission ID') + dd").textContent();
        // Click "Select Data Source for Permission"
        page.getByRole(AriaRole.LISTBOX).getByText("Select Data Source").click();
        page.getByRole(AriaRole.OPTION).getByText("Simulation").first().click();
        // Click "Accept" button
        page.getByRole(AriaRole.BUTTON).getByText("Accept").click();
        // Wait for permission to be added to the list
        page.waitForTimeout(1000);
        // Visible "FUTURE_NEAR_REALTIME_DATA" in list
        var permission = page.getByRole(AriaRole.LISTITEM).first();
        // Show details
        permission.getByRole(AriaRole.HEADING).click();
        // Check if permission has the right data need and permission ID
        assertThat(permission).containsText(dataNeed);
        assertThat(permission).containsText(permissionId);
        // Check if status is correct
        assertThat(permission).containsText("Streaming Data");
        // Revoke permission
        permission.getByRole(AriaRole.BUTTON).getByText("Revoke").click();
        page.getByRole(AriaRole.DIALOG).getByRole(AriaRole.BUTTON).getByText("Revoke").click();
        // Navigate to complete tab
        page.getByRole(AriaRole.BUTTON).getByText("Complete").click();
        // Check if permission shows up there
        permission = page.getByRole(AriaRole.LISTITEM).first();
        permission.getByRole(AriaRole.HEADING).click();
        // Check if permission has the right data need and permission ID
        assertThat(permission).containsText(dataNeed);
        assertThat(permission).containsText(permissionId);
        // Check if permission has revoked status
        assertThat(permission).containsText("Revoked");

        // Delete data source
        page.getByRole(AriaRole.LINK).getByText("Data Sources").click();
        dataSourceCard = page.getByRole(AriaRole.ARTICLE).filter(
                new Locator.FilterOptions().setHasText(dataSource));
        dataSourceCard.getByRole(AriaRole.HEADING).click();
        dataSourceCard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete")).click();
        page.getByRole(AriaRole.DIALOG)
            .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete"))
            .click();
        assertThat(dataSourceCard).isHidden();
    }

    @Test
    void logout(Page page) {
        page.getByText("Account").click();
        page.getByText("Logout").click();
        assertThat(page.getByText("Sign in to your account")).isVisible();
    }

    @AfterEach
    void saveScreenshot(Page page, TestInfo testInfo) {
        var screenshotPath = Paths.get("build/test-results/test",
                                       getClass().getSimpleName(),
                                       testInfo.getDisplayName() + ".png");
        page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
    }
}

const RELEASE_STATUS = {
  unknown: { variant: "neutral", icon: "❓" },
  deployed: { variant: "success", icon: "✅" },
  uninstalled: { variant: "warning", icon: "⚠️" },
  superseded: { variant: "primary", icon: "🔄" },
  failed: { variant: "danger", icon: "❌" },
  uninstalling: { variant: "warning", icon: "⏳" },
  "pending-install": { variant: "info", icon: "🕒" },
  "pending-upgrade": { variant: "info", icon: "🔼" },
  "pending-rollback": { variant: "info", icon: "🔽" },
};

const INSTALLER_BASE_URL = document
  .querySelector('meta[name="installer-base-url"]')
  .getAttribute("content");
const INSTALLER_HEALTH_URL = `${INSTALLER_BASE_URL}/health`;
const INSTALLER_AIIDA_URL = `${INSTALLER_BASE_URL}/aiida`;
const INSTALLER_SERVICES_BASE_URL = `${INSTALLER_BASE_URL}/services/user`;

const generalErrorContainer = document.getElementById(
  "general-error-container"
);

const aiidaLoadingBar = document.getElementById("aiida-loading-bar");
const aiidaErrorContainer = document.getElementById("aiida-error-container");
const aiidaVersionInfoElement = document.getElementById("aiida-version-info");

const servicesNewChartName = document.getElementById(
  "services-install-new-chart-name"
);
const servicesNewCustomValues = document.getElementById(
  "services-install-new-custom-values"
);

const servicesLoadingBar = document.getElementById("services-loading-bar");
const servicesErrorContainer = document.getElementById(
  "services-error-container"
);
const servicesVersionInfosList = document.getElementById(
  "services-version-infos"
);

function getCsrfHeader() {
  return document
    .querySelector('meta[name="csrf-header"]')
    .getAttribute("content");
}

function getCsrfToken() {
  return document
    .querySelector('meta[name="csrf-token"]')
    .getAttribute("content");
}

function renderErrorMessage(errorMessage, errorContainer, loadingBar) {
  errorContainer.innerHTML = /* HTML */ `
    <sl-alert variant="danger" open class="error-alert">
      <sl-icon slot="icon" name="exclamation-triangle-fill"></sl-icon>
      <strong>Error: </strong>${errorMessage}
    </sl-alert>
  `;

  if (loadingBar) loadingBar.style.visibility = "hidden";
}

async function checkResponse(response, errorContainer, loadingBar) {
  if (loadingBar) loadingBar.style.visibility = "visible";

  let data;
  try {
    data = await response.json();
  } catch (error) {}

  if (loadingBar) loadingBar.style.visibility = "hidden";

  if (!response.ok) {
    const errors = data.errors.map((error) => error.message).join("\n");
    renderErrorMessage(errors, errorContainer, loadingBar);
    return Promise.reject(new Error(errors));
  }

  return data;
}

function health() {
  return fetch(INSTALLER_HEALTH_URL).then((response) =>
    checkResponse(response, generalErrorContainer, null)
  );
}

function aiidaVersionInfo() {
  return fetch(INSTALLER_AIIDA_URL).then((response) =>
    checkResponse(response, aiidaErrorContainer, aiidaLoadingBar)
  );
}

function servicesVersionInfos() {
  return fetch(INSTALLER_SERVICES_BASE_URL).then((response) =>
    checkResponse(response, servicesErrorContainer, servicesLoadingBar)
  );
}

function getChartServicePath(chartName) {
  return `${INSTALLER_SERVICES_BASE_URL}/${chartName}`;
}

function getReleaseServicePath(chartName, releaseName) {
  return `${INSTALLER_SERVICES_BASE_URL}/${chartName}/${releaseName}`;
}

function installOrUpgrade(chartName, releaseName, isCore) {
  let url, errorContainer, successCallback, loadingBar;

  if (isCore) {
    url = INSTALLER_AIIDA_URL;
    errorContainer = aiidaErrorContainer;
    successCallback = renderAiidaVersionInfo;
    loadingBar = aiidaLoadingBar;
  } else {
    url = getReleaseServicePath(chartName, releaseName);
    errorContainer = servicesErrorContainer;
    successCallback = renderServicesVersionInfos;
    loadingBar = servicesLoadingBar;
  }

  loadingBar.style.visibility = "visible";

  fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
  })
    .then((response) => checkResponse(response, errorContainer, loadingBar))
    .then((_) => successCallback())
    .catch((error) =>
      renderErrorMessage(error.message, errorContainer, loadingBar)
    );
}

function installNewService(event) {
  event.preventDefault();

  const chartName = servicesNewChartName.value;
  const customValues = servicesNewCustomValues.value;

  let setupDto = null;
  if(customValues){
    setupDto = JSON.stringify({"customValues": customValues.split("\n")})
  }
  console.log(setupDto)

  servicesLoadingBar.style.visibility = "visible";

  fetch(getChartServicePath(chartName), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
    body: setupDto
  })
    .then((response) =>
      checkResponse(response, servicesErrorContainer, servicesLoadingBar)
    )
    .then((_) => renderServicesVersionInfos())
    .catch((error) =>
      renderErrorMessage(
        error.message,
        servicesErrorContainer,
        servicesLoadingBar
      )
    );
}

function uninstallService(chartName, releaseName) {
  servicesLoadingBar.style.visibility = "visible";

  fetch(getReleaseServicePath(chartName, releaseName), {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
  })
    .then((response) =>
      checkResponse(response, servicesErrorContainer, servicesLoadingBar)
    )
    .then((_) => renderServicesVersionInfos())
    .catch((error) =>
      renderErrorMessage(
        error.message,
        servicesErrorContainer,
        servicesLoadingBar
      )
    );
}

function toLocalDateString(time) {
  return time ? new Date(time).toLocaleString() : "N/A";
}

function versionInfoElement(versionInfo, isCore = false) {
  const { releaseName, releaseInfo, installedChart, latestChart } = versionInfo;

  const installedVersion = `${installedChart.version} (${latestChart.appVersion})`;
  const latestVersion = `${latestChart.version} (${latestChart.appVersion})`;

  const isLatest = installedVersion === latestVersion;

  const statusInfo = RELEASE_STATUS[releaseInfo.status] || {
    variant: "neutral",
    icon: "❓",
  };
  const versionVariant = isLatest ? "success" : "warning";

  return /* HTML */ `
    <sl-card class="version-info-element">
      <div class="version-info-header">
        <strong>${installedChart.name}</strong>
        <div>
          <sl-badge variant="${versionVariant}">${installedVersion}</sl-badge>
          <sl-badge variant="${statusInfo.variant}">
            ${statusInfo.icon} ${releaseInfo.status.toUpperCase()}
          </sl-badge>
        </div>
      </div>

      <small class="version-info-label">${installedChart.description}</small>

      <sl-details class="version-info-details" summary="More details">
        <dl class="version-info-list">
          <dt>Deployment Name</dt>
          <dd>${releaseName}</dd>
          <dt>First Deployed</dt>
          <dd>${toLocalDateString(releaseInfo.firstDeployed)}</dd>
          <dt>Last Deployed</dt>
          <dd>${toLocalDateString(releaseInfo.lastDeployed)}</dd>
          <dt>Deployment Description</dt>
          <dd>${releaseInfo.description}</dd>
        </dl>

        <div class="version-info-actions">
          <sl-badge>Latest: ${latestVersion}</sl-badge>
          <div>
            <sl-button
              size="small"
              variant="primary"
              ${isLatest ? "disabled" : ""}
              onclick="window.installOrUpgrade('${installedChart.name}', '${releaseName}', ${isCore})"
            >
              Upgrade
            </sl-button>
            <sl-button
              size="small"
              variant="danger"
              ${isCore ? "disabled" : ""}
              onclick="window.uninstallService('${installedChart.name}', '${releaseName}')"
            >
              Uninstall
            </sl-button>
          </div>
        </div>
      </sl-details>
    </sl-card>
  `;
}

function renderAiidaVersionInfo() {
  aiidaLoadingBar.style.visibility = "visible";

  aiidaVersionInfo().then((aiidaVersionInfo) => {
    aiidaVersionInfoElement.innerHTML = versionInfoElement(
      aiidaVersionInfo,
      true
    );
    aiidaLoadingBar.style.visibility = "hidden";
  });
}

function renderServicesVersionInfos() {
  servicesLoadingBar.style.visibility = "visible";

  servicesVersionInfos().then((servicesVersionInfos) => {
    servicesVersionInfosList.innerHTML = servicesVersionInfos
      .map((versionInfo) => versionInfoElement(versionInfo))
      .join("");

    if (servicesVersionInfos.length === 0) {
      servicesVersionInfosList.innerHTML = /* HTML */ `
        <sl-alert variant="primary" open class="alert">
          <sl-icon slot="icon" name="info-circle-fill"></sl-icon>
          <strong>No services installed</strong>
        </sl-alert>
      `;
    }

    servicesLoadingBar.style.visibility = "hidden";
  });
}

Promise.all([
  customElements.whenDefined("sl-button"),
  customElements.whenDefined("sl-input"),
]).then(() => {
  document
    .getElementById("services-install-new-form")
    .addEventListener("submit", installNewService);
});

health();
renderAiidaVersionInfo();
renderServicesVersionInfos();

window.installOrUpgrade = installOrUpgrade;
window.uninstallService = uninstallService;

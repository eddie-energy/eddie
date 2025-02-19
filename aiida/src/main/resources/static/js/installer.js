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

const INSTALLER_BASE_URL = "/installer";
const INSTALLER_HEALTH_URL = `${INSTALLER_BASE_URL}/health`;
const INSTALLER_AIIDA_URL = `${INSTALLER_BASE_URL}/aiida`;
const INSTALLER_SERVICES_BASE_URL = `${INSTALLER_BASE_URL}/services/user`;

const generalErrorContainer = document.getElementById(
  "general-error-container"
);
const aiidaErrorContainer = document.getElementById("aiida-error-container");
const servicesErrorContainer = document.getElementById(
  "services-error-container"
);

const aiidaVersionInfoElement = document.getElementById("aiida-version-info");
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

async function checkResponse(response, errorContainer) {
  const data = await response.json();

  if (!response.ok) {
    const errors = data.errors.map((error) => error.message).join("\n");

    errorContainer.innerHTML = /* HTML */ `
      <sl-alert variant="danger" open>
        <sl-icon slot="icon" name="exclamation-triangle-fill"></sl-icon>
        <strong>Error: </strong>${errors}
      </sl-alert>
    `;
  }

  return data;
}

function health() {
  return fetch(INSTALLER_HEALTH_URL).then((response) =>
    checkResponse(response, generalErrorContainer)
  );
}

function aiidaVersionInfo() {
  return fetch(INSTALLER_AIIDA_URL).then((response) =>
    checkResponse(response, aiidaErrorContainer)
  );
}

function servicesVersionInfos() {
  return fetch(INSTALLER_SERVICES_BASE_URL).then((response) =>
    checkResponse(response, servicesErrorContainer)
  );
}

function installOrUpgrade(releaseName, isCore) {
  console.log(`Install or upgrade ${releaseName} (isCore: ${isCore})`);

  const url = isCore
    ? `/installer/aiida`
    : `/installer/services/${releaseName}`;

  fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("Failed to install or upgrade");
      }
      return response.json();
    })
    .then((data) => {
      console.log(data);
      renderVersionInfos();
    })
    .catch((error) => {
      console.error(error);
    });
}

function uninstall(releaseName) {}

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
          <dd>${toLocalDateString(releaseInfo.first_deployed)}</dd>
          <dt>Last Deployed</dt>
          <dd>${toLocalDateString(releaseInfo.last_deployed)}</dd>
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
              onclick="window.installOrUpgrade('${releaseName}', ${isCore})"
            >
              Upgrade
            </sl-button>
            <sl-button
              size="small"
              variant="danger"
              ${isCore ? "disabled" : ""}
              onclick="window.uninstall('${releaseName}')"
            >
              Uninstall
            </sl-button>
          </div>
        </div>
      </sl-details>
    </sl-card>
  `;
}

function renderVersionInfos() {
  aiidaVersionInfo().then((aiidaVersionInfo) => {
    aiidaVersionInfoElement.innerHTML = versionInfoElement(
      aiidaVersionInfo,
      true
    );
  });

  servicesVersionInfos().then((servicesVersionInfos) => {
    servicesVersionInfosList.innerHTML = servicesVersionInfos.length
      ? servicesVersionInfos
          .map((versionInfo) => versionInfoElement(versionInfo))
          .join("")
      : "No services installed";
  });
}

health();
renderVersionInfos();

window.installOrUpgrade = installOrUpgrade;
window.uninstall = uninstall;

const STATUS = {
  ACCEPTED: {
    title: "Accepted",
    description:
      "You accepted the permission request and it is being processed.",
    isActive: true,
    isRevocable: true,
  },
  WAITING_FOR_START: {
    title: "Waiting for Start",
    description:
      "You accepted the permission request and it is scheduled to start at the specified start time.",
    isActive: true,
    isRevocable: true,
  },
  STREAMING_DATA: {
    title: "Streaming Data",
    description:
      "You accepted the permission request and it is now actively streaming data to the eligible party.",
    isActive: true,
    isRevocable: true,
  },
  REJECTED: {
    title: "Rejected",
    description: "You rejected the permission request.",
  },
  REVOCATION_RECEIVED: {
    title: "Revocation Received",
    description: "You requested revocation of the permission.",
    isActive: true,
  },
  REVOKED: {
    title: "Revoked",
    description: "You revoked the permission.",
  },
  TERMINATED: {
    title: "Terminated",
    description: "The permission was terminated by the eligible party.",
  },
  FULFILLED: {
    title: "Fulfilled",
    description: "The expiration time of the permission was reached.",
  },
  FAILED_TO_START: {
    title: "Failed to Start",
    description: "An error occurred and the permission could not be started.",
  },
  CREATED: {
    title: "Created",
    description:
      "The permission has been created, but the details have not yet been fetched from the EDDIE framework.",
    isActive: true,
  },
  FETCHED_DETAILS: {
    title: "Fetched details",
    description: "This permission waits for you to accept or reject it.",
    isActive: true,
  },
  UNFULFILLABLE: {
    title: "Unable to fulfill",
    description:
      "Your AIIDA instance is unable to fulfill the permission request, e.g. because the requested data is not available on your AIIDA instance.",
  },
  FETCHED_MQTT_CREDENTIALS: {
    title: "Fetched MQTT details",
    description:
      "This is an internal state only, the permission should be transitioned into another state automatically.",
    isActive: true,
  },
};

const PERMISSIONS_BASE_URL = document
  .querySelector('meta[name="permissions-base-url"]')
  .getAttribute("content");
const DATASOURCES_BASE_URL = document
  .querySelector('meta[name="datasources-base-url"]')
  .getAttribute("content");
const MQTT_BASE_URL = document
  .querySelector('meta[name="mqtt-base-url"]')
  .getAttribute("content");

const permissionDialog = document.getElementById("permission-dialog");
const permissionDialogContent = document.getElementById(
  "permission-dialog-content"
);

const userDrawer = document.getElementById("user-drawer");

const acceptButton = document.getElementById("permission-dialog-accept");
const rejectButton = document.getElementById("permission-dialog-reject");
const closeButton = document.getElementById("permission-dialog-close");

const revokeDialog = document.getElementById("revoke-permission-dialog");
const revokeButton = document.getElementById("revoke-permission-button");

const aiidaCodeInput = document.getElementById("aiida-code");
/** @type {HTMLFormElement} */
const permissionForm = document.getElementById("permission-form");

const activePermissionsList = document.getElementById("active-permissions");
const expiredPermissionsList = document.getElementById("expired-permissions");

// user has to either accept or reject a new permission, don't allow closing of dialog
permissionDialog.addEventListener("sl-request-close", (event) => {
  event.preventDefault();
});

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

function permissions() {
  return fetch(PERMISSIONS_BASE_URL).then((response) => response.json());
}

function handlePermissionFormSubmit(event) {
  event.preventDefault();

  const permission = JSON.parse(atob(aiidaCodeInput.value));

  const { serviceName } = permission;

  permissionDialogContent.innerHTML = /* HTML */ `
    <h3>Loading details for service</h3>
    <h3>
      <strong>${serviceName}</strong>
    </h3>
  `;

  addPermission(permission);
  acceptButton.loading = true;
  rejectButton.loading = true;
  closeButton.loading = true;
  acceptButton.disabled = true;
  rejectButton.disabled = true;
  closeButton.disabled = true;

  permissionDialog.show();
}

aiidaCodeInput.addEventListener("sl-input", () => {
  try {
    // reset validity
    aiidaCodeInput.setCustomValidity("");
    // check if input can be parsed into the correct format
    // noinspection JSUnusedLocalSymbols
    const { eddieId, permissionId, serviceName, handshakeUrl, accessToken } =
      JSON.parse(atob(aiidaCodeInput.value));
  } catch (error) {
    console.debug(error);
    aiidaCodeInput.setCustomValidity(
      "Please confirm you entered a valid AIIDA code"
    );
  }
});

function toLocalDateString(time) {
  return new Date(time).toLocaleString();
}

function permissionElement(permission) {
  const notYetAvailable = "Not available yet.";

  const { eddieId, permissionId, status, serviceName } = permission;
  const dataTags = permission.dataNeed.dataTags ?? notYetAvailable;
  const startTime = toLocalDateString(permission.startTime) ?? notYetAvailable;
  const expirationTime =
    toLocalDateString(permission.expirationTime) ?? notYetAvailable;
  const transmissionSchedule =
    permission.dataNeed.transmissionSchedule ?? notYetAvailable;
  const asset = permission.dataNeed.asset ?? notYetAvailable;
  const schemas = permission.dataNeed.schemas ?? notYetAvailable;

  return /* HTML */ `
    <sl-details>
      <span slot="summary">
        <strong>${serviceName}</strong><br />
        <small class="label">${permissionId}</small>
      </span>

      <dl class="details-list">
        <dt>Service</dt>
        <dd>${serviceName}</dd>
        <dt>Status</dt>
        <dd>
          <sl-tooltip content="${STATUS[status].description}">
            <sl-badge
              variant="${STATUS[status].isActive ? "success" : "danger"}"
            >
              ${STATUS[status].title}
            </sl-badge>
          </sl-tooltip>
        </dd>

        <dt>EDDIE Application</dt>
        <dd>${eddieId}</dd>

        <dt>Permission ID</dt>
        <dd>${permissionId}</dd>

        <dt>Start</dt>
        <dd>${startTime}</dd>

        <dt>End</dt>
        <dd>${expirationTime}</dd>

        <dt>Transmission Schedule</dt>
        <dd>${window.cronstrue.toString(transmissionSchedule)}</dd>

        <dt>Schemas</dt>
        <dd>
          ${schemas.map((schema) => `<span>${schema}</span>`).join("<br>")}
        </dd>

        <dt>Asset</dt>
        <dd>${asset}</dd>

        ${dataTags && dataTags.length
          ? `
                <dt>OBIS-Codes</dt>
                <dd>${dataTags.map((code) => `<span>${code}</span>`).join("<br>")}</dd>
            `
          : ""}

        <dt>Data Source</dt>
        <dd>
          ${permission.dataSource
            ? `${permission.dataSource.name} (${permission.dataSource.id})`
            : "Not found."}
        </dd>
      </dl>
      ${STATUS[status].isRevocable
        ? /* HTML */ `
            <sl-button
              style="margin-top: 1rem"
              onclick="window.openRevokePermissionDialog('${permissionId}')"
            >
              Revoke
            </sl-button>
          `
        : ""}
    </sl-details>
  `;
}

function renderPermissions() {
  permissions().then((permissions) => {
    activePermissionsList.innerHTML = "";
    expiredPermissionsList.innerHTML = "";

    permissions.forEach((permission) => {
      const element = permissionElement(permission);

      if (STATUS[permission.status].isActive) {
        activePermissionsList.insertAdjacentHTML("beforeend", element);
      } else {
        expiredPermissionsList.insertAdjacentHTML("beforeend", element);
      }
    });
  });
}

function addPermission(permission) {
  fetch(PERMISSIONS_BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
    body: JSON.stringify(permission),
  })
    .then((response) => {
      if (!response.ok) {
        return response.json().then((json) => {
          throw new Error(
            `Failed to add permission: ${json.errors[0].message}`
          );
        });
      }
      return response.json();
    })
    .then((permission) => {
      updatePermissionDialogWithDetails(permission);
    })
    .catch((error) => {
      console.debug(error);

      permissionDialogContent.insertAdjacentHTML(
        "beforeend",
        /* HTML */ ` <br />
          <sl-alert variant="danger" open>
            <sl-icon slot="icon" name="exclamation-octagon"></sl-icon>
            <p>
              There was an error confirming the permission request. Please
              contact the service provider if this issue persists.
            </p>
          </sl-alert>`
      );

      acceptButton.loading = rejectButton.loading = closeButton.loading = false;
      closeButton.disabled = false;
    });
}

function updatePermission(operation) {
  console.debug(operation + " permission");

  const { permissionId } = JSON.parse(atob(aiidaCodeInput.value));

  const dataSourceSelect = document.getElementById("data-source-select");
  const dataSourceId = dataSourceSelect.value;

  fetch(`${PERMISSIONS_BASE_URL}/${permissionId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
    body: JSON.stringify({
      operation: operation,
      dataSourceId: dataSourceId,
    }),
  }).then(() => {
    permissionDialog.hide();
    aiidaCodeInput.value = "";
    renderPermissions();
  });
}

function updatePermissionDialogWithDetails(permission) {
  console.debug("Updating dialog with details", permission);

  const {
    eddieId,
    permissionId,
    serviceName,
    startTime,
    expirationTime,
    dataNeed: { dataTags, transmissionSchedule, schemas, asset },
  } = permission;

  fetch(`${DATASOURCES_BASE_URL}`)
    .then((response) => response.json())
    .then((dataSources) => {
      permissionDialogContent.innerHTML = /* HTML */ `
        <span>Permission request from</span>

        <h3>
          <strong>${serviceName}</strong>
        </h3>

        <dl class="permission-details">
          <dt>Service</dt>
          <dd>${serviceName}</dd>

          <dt>EDDIE Application</dt>
          <dd>${eddieId}</dd>

          <dt>Permission ID</dt>
          <dd>${permissionId}</dd>

          <dt>Start</dt>
          <dd>${toLocalDateString(startTime)}</dd>

          <dt>End</dt>
          <dd>${toLocalDateString(expirationTime)}</dd>

          <dt>Transmission Schedule</dt>
          <dd>${window.cronstrue.toString(transmissionSchedule)}</dd>

          <dt>Schemas</dt>
          <dd>
            ${schemas.map((schema) => `<span>${schema}</span>`).join("<br>")}
          </dd>

          <dt>Asset</dt>
          <dd>${asset}</dd>

          ${dataTags && dataTags.length
            ? `
            <dt>OBIS-Codes</dt>
            <dd>${dataTags.map((code) => `<span>${code}</span>`).join("<br>")}</dd>
        `
            : ""}
        </dl>

        <sl-select
          id="data-source-select"
          name="data-source"
          label="Data Source"
          required
        >
          ${dataSources
            .map(
              (dataSource) =>
                `<sl-option value="${dataSource.id}">${dataSource.name} (${dataSource.id})</sl-option>`
            )
            .join("")}
        </sl-select>

        <p class="text">
          <em>${serviceName}</em> requests permission to retrieve the
          near-realtime data for the given time frame and OBIS-codes. Please
          confirm the request is correct before granting permission.
        </p>
      `;

      const dataSourceSelect = document.getElementById("data-source-select");
      dataSourceSelect.value = dataSources[0].id;
    });

  acceptButton.loading = rejectButton.loading = closeButton.loading = false;
  acceptButton.disabled = rejectButton.disabled = closeButton.disabled = false;
}

function openRevokePermissionDialog(permissionId) {
  revokeDialog.show();
  revokeButton.onclick = () => {
    revokePermission(permissionId);
    revokeDialog.hide();
  };
}

function revokePermission(permissionId) {
  fetch(`${PERMISSIONS_BASE_URL}/${permissionId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
    body: JSON.stringify({
      operation: "REVOKE",
    }),
  }).then(() => renderPermissions());
}

function renderDataSources() {
  fetch(`${DATASOURCES_BASE_URL}`)
    .then((response) => response.json())
    .then((dataSources) => {
      const dataSourceList = document.getElementById("data-source-list");
      dataSourceList.innerHTML = "";

      dataSources.forEach((dataSource) => {
        const template = document.createElement("template");
        if (dataSource.dataSourceType === "SIMULATION") {
          renderSimulationDataSource(template, dataSource, dataSourceList);
        } else if (dataSource.dataSourceType === "MODBUS") {
          renderModbusDataSource(template, dataSource, dataSourceList);
        } else {
          renderMqttDataSource(template, dataSource, dataSourceList);
        }
      });

      function renderModbusDataSource(template, dataSource, dataSourceList) {
        let dataSourceTypeDetails = `
                <dt>Polling Interval:</dt>
                <dd>${dataSource.simulationPeriod} seconds</dd>
                <dt>Modbus IP:</dt>
                <dd>${dataSource.modbusSettings.modbusIp}</dd>
              `;

        appendDataSourceToChild(
          dataSource,
          template,
          dataSourceTypeDetails,
          dataSourceList
        );
      }

      function renderSimulationDataSource(
        template,
        dataSource,
        dataSourceList
      ) {
        let dataSourceTypeDetails = /* HTML */ `
          <dt>Simulation Period:</dt>
          <dd>${dataSource.simulationPeriod} seconds</dd>
        `;

        appendDataSourceToChild(
          dataSource,
          template,
          dataSourceTypeDetails,
          dataSourceList
        );
      }

      function renderMqttDataSource(template, dataSource, dataSourceList) {
        let dataSourceTypeDetails = /* HTML */ `
          <dt>MQTT Server URI:</dt>
          <dd>${dataSource.mqttSettings.externalHost}</dd>

          <dt>MQTT Topic:</dt>
          <dd>${dataSource.mqttSettings.subscribeTopic}</dd>

          <dt>MQTT Username:</dt>
          <dd>${dataSource.mqttSettings.username}</dd>

          <dt>MQTT Password:</dt>
          <dd>
            <sl-button
              variant="default"
              size="small"
              onclick="window.regenerateSecretsDataSource('${dataSource.id}')"
            >
              Regenerate
            </sl-button>
          </dd>
          <dt>Certificate:</dt>
          <dd>
            <sl-button
              variant="default"
              size="small"
              onclick="window.downloadTlsCertificate()"
            >
              Download
            </sl-button>
          </dd>
        `;

        appendDataSourceToChild(
          dataSource,
          template,
          dataSourceTypeDetails,
          dataSourceList
        );
      }

      function appendDataSourceToChild(
        dataSource,
        template,
        dataSourceTypeDetails,
        dataSourceList
      ) {
        template.innerHTML = /* HTML */ `
          <sl-card>
            <h3>${dataSource.countryCode} - ${dataSource.name}</h3>

            <dl class="details-list">
              <dt>ID:</dt>
              <dd>${dataSource.id}</dd>

              <dt>Asset:</dt>
              <dd>${dataSource.asset}</dd>

              <dt>Type:</dt>
              <dd>${dataSource.dataSourceType}</dd>

              ${dataSourceTypeDetails}

              <dt>Enabled:</dt>
              <dd>${dataSource.enabled}</dd>
            </dl>
            <sl-button class="edit-button" data-id="${dataSource.id}">
              Edit
            </sl-button>
            <sl-button class="delete-button" data-id="${dataSource.id}">
              Delete
            </sl-button>
          </sl-card>
        `;

        dataSourceList.appendChild(template.content);
      }

      document.querySelectorAll(".delete-button").forEach((button) => {
        button.addEventListener("click", (event) => {
          deleteDataSource(event.target.dataset.id);
        });
      });

      document.querySelectorAll(".toggle-enabled").forEach((toggle) => {
        toggle.addEventListener("sl-change", (event) => {
          const newEnabledState = event.target.checked;
          updateEnabledState(event.target.dataset.id, newEnabledState);
        });
      });

      document.querySelectorAll(".edit-button").forEach((button) => {
        button.addEventListener("click", (event) => {
          openEditDataSourceDialog(event.target.dataset.id);
        });
      });
    })
    .catch((error) => {
      console.error("Failed to fetch data sources:", error);
    });
}

function deleteDataSource(dataSourceId) {
  fetch(`${DATASOURCES_BASE_URL}/${dataSourceId}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("Failed to delete data source");
      }
      console.log(`Data source with ID ${dataSourceId} deleted.`);
      renderDataSources(); // Refresh the list after deletion
    })
    .catch((error) => {
      console.error("Failed to delete data source:", error);
    });
}

function updateEnabledState(dataSourceId, enabled) {
  fetch(`${DATASOURCES_BASE_URL}/${dataSourceId}/enabled`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
    body: JSON.stringify(enabled),
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("Failed to update enabled state");
      }
      console.log(
        `Enabled state for data source with ID ${dataSourceId} updated to ${enabled}.`
      );
    })
    .catch((error) => {
      console.error("Failed to update enabled state:", error);
    });
}

function openAddDataSourceDialog() {
  const form = document.getElementById("add-data-source-form");

  form.innerHTML = /* HTML */ ` <sl-input
      name="name"
      label="Name"
      required
    ></sl-input>
    <br />
    <sl-select
      id="country-code-type"
      name="countryCode"
      label="Country"
      required
    >
    </sl-select>
    <br />
    <sl-checkbox name="enabled" checked>Enabled</sl-checkbox>
    <br />
    <br />
    <sl-select id="asset-type" label="Asset Type"></sl-select>
    <br />
    <sl-select
      id="data-source-type"
      name="dataSourceType"
      label="Data Source Type"
      required
    >
    </sl-select>
    <br />
    <div id="data-source-fields"></div>`;

  const dialog = document.getElementById("add-data-source-dialog");
  const supportedCountryCodes = ["AT", "FR", "NL"];
  const countryCodeSelect = document.getElementById("country-code-type");
  countryCodeSelect.innerHTML = supportedCountryCodes
    .map(
      (countryCode) =>
        `<sl-option value="${countryCode}">${countryCode}</sl-option>`
    )
    .join("");
  const dataSourceSelect = document.getElementById("data-source-type");
  const assetSelect = document.getElementById("asset-type");

  fetch(`${DATASOURCES_BASE_URL}/outbound/types`)
    .then((response) => response.json())
    .then((types) => {
      dataSourceSelect.innerHTML = types
        .map(
          (type) =>
            `<sl-option value="${type.identifier}">${type.name}</sl-option>`
        )
        .join("");

      dataSourceSelect.addEventListener("sl-change", (event) => {
        const selectedValue = event.target.value;
        const selectedType = types.find(
          (type) => type.identifier === selectedValue
        );
        if (!selectedType) {
          console.error(`Type not found for value: ${selectedValue}`);
          return;
        }

        updateDataSourceFields(selectedType.identifier);
      });

      if (types.length > 0) {
        dataSourceSelect.value = types[0].identifier;
        updateDataSourceFields(types[0].identifier);
      }
    })
    .catch((error) =>
      console.error("Failed to fetch data source types:", error)
    );

  fetch(`${DATASOURCES_BASE_URL}/assets`)
    .then((response) => response.json())
    .then((data) => {
      const assets = data.assets;
      assetSelect.innerHTML = assets
        .map((asset) => `<sl-option value="${asset}">${asset}</sl-option>`)
        .join("");

      if (assets.length > 0) {
        assetSelect.value = assets[0];
      }
    })
    .catch((error) => console.error("Failed to fetch assets:", error));

  dialog.show();
}

function updateDataSourceFields(type) {
  const dataSourceFields = document.getElementById("data-source-fields");

  let dataTypeFields = "";
  if (type === "SIMULATION") {
    dataTypeFields += `<br /><sl-input name="simulationPeriod" label="Simulation Period" type="number" required></sl-input>`;
  }

  dataSourceFields.innerHTML = dataTypeFields;

  if (type === "MODBUS") {
    createModbusFields(dataSourceFields);
  }
}

function closeAddDataSourceDialog() {
  document.getElementById("add-data-source-dialog").hide();
}

function regenerateSecretsDataSource(dataSourceId) {
  fetch(`${DATASOURCES_BASE_URL}/${dataSourceId}/regenerate-secrets`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
  })
    .then(async (response) => {
      if (!response.ok) {
        const errorMessage = await response.text();
        throw new Error(
          `Failed to regenerate secrets: [${response.status}] ${errorMessage}`
        );
      }
      return response.json();
    })
    .then((dataSourceSecrets) => {
      openSecretsDataSourceDialog(dataSourceSecrets);
    })
    .catch((error) => {
      console.error("Failed to regenerate secrets:", error);
      alert(`Failed to regenerate secrets: ${error.message}`);
    });
}

function downloadTlsCertificate() {
  const url = `${MQTT_BASE_URL}/download/tls-certificate`;

  fetch(url, { method: "HEAD" }).then(async (response) => {
    if (!response.ok) {
      alert("No TLS certificate found!");
      return;
    }

    window.location.href = url;
  });
}

function openSecretsDataSourceDialog(dataSourceSecrets) {
  closeAddDataSourceDialog();
  renderDataSources();

  if (dataSourceSecrets && dataSourceSecrets.plaintextPassword) {
    const passwordSpan = document.getElementById(
      `secrets-data-source-password`
    );
    const toggleIcon = document.getElementById(
      `secrets-data-source-toggle-password`
    );

    passwordSpan.innerText = dataSourceSecrets.plaintextPassword;

    if (toggleIcon) {
      toggleIcon.addEventListener("click", () => {
        const present = passwordSpan.toggleAttribute("hidden");
        toggleIcon.setAttribute("name", present ? "eye" : "eye-slash");
      });
    }

    document.getElementById("secrets-data-source-dialog").show();
  }
}

function closeSecretsDataSourceDialog() {
  document.getElementById("secrets-data-source-dialog").hide();
}

function openEditDataSourceDialog(dataSourceId) {
  fetch(`${DATASOURCES_BASE_URL}/${dataSourceId}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
  })
    .then((response) => response.json())
    .then((dataSource) => {
      const editDataSourceFields = document.getElementById(
        "edit-data-source-fields"
      );

      Promise.all([
        fetch(`${DATASOURCES_BASE_URL}/assets`).then((response) =>
          response.json()
        ),
      ])
        .then(([data]) => {
          const assets = data.assets;
          let editFields = /* HTML */ `
            <sl-input
              name="name"
              label="Name"
              value="${dataSource.name}"
              required
            ></sl-input>
            <br />
            <input
              name="countryCode"
              value="${dataSource.countryCode}"
              type="hidden"
            />
            <sl-checkbox name="enabled" ${dataSource.enabled ? "checked" : ""}>
              Enabled
            </sl-checkbox>
            <br />
            <br />
            <sl-select id="asset-select" name="asset" label="Asset" required>
              ${assets
                .map(
                  (asset) => `<sl-option value="${asset}">${asset}</sl-option>`
                )
                .join("")}
            </sl-select>
            <input
              name="dataSourceType"
              value="${dataSource.dataSourceType}"
              type="hidden"
            />
          `;

          if (dataSource.dataSourceType === "SIMULATION") {
            editFields += /* HTML */ `
              <br />
              <sl-input
                name="simulationPeriod"
                label="Simulation Period"
                type="number"
                value="${dataSource.simulationPeriod}"
                required
              ></sl-input>
            `;
          } else if (dataSource.dataSourceType === "MODBUS") {
            editFields += /* HTML */ `
              <br />
              <sl-input
                name="modbusIp"
                label="Local IP Address"
                value="${dataSource.modbusSettings.modbusIp}"
                required
              ></sl-input>
            `;
          }

          editDataSourceFields.innerHTML = editFields;

          const assetSelect = document.getElementById("asset-select");
          assetSelect.value = dataSource.asset;

          document
            .getElementById("edit-data-source-form")
            .setAttribute("data-id", dataSourceId);

          document.getElementById("edit-data-source-dialog").show();
        })
        .catch((error) => {
          console.error("Failed to fetch types or assets:", error);
        });
    })
    .catch((error) => {
      console.error("Failed to fetch data source details:", error);
    });
}

function closeEditDataSourceDialog() {
  document.getElementById("edit-data-source-dialog").hide();
}

function createModbusFields(dataSourceFields) {
  dataSourceFields.innerHTML = `
    <sl-input
      id="modbus-ip"
      name="modbusIp"
      label="Local IP Address"
      placeholder="e.g. 192.168.x.x / localhost"
      required
      help-text="Enter a private local IP address (e.g. 192.168.x.x)"
    ></sl-input>
    <br />
    <sl-select
      id="modbus-vendor-list"
      name="modbusVendor"
      label="Vendor"
      placeholder="Select a vendor..."
      required
    ></sl-select>
    <br />
    <sl-select
      id="modbus-model-list"
      name="modbusModel"
      label="Model"
      placeholder="Select a model..."
      required
      disabled
    ></sl-select>
    <br />
    <sl-select
      id="modbus-device-list"
      name="modbusDevice"
      label="Device"
      placeholder="Select a device..."
      required
      disabled
    ></sl-select>
  `;

  const ipInput = document.getElementById("modbus-ip");
  const vendorSelect = document.getElementById("modbus-vendor-list");
  const modelSelect = document.getElementById("modbus-model-list");
  const deviceSelect = document.getElementById("modbus-device-list");

  ipInput.addEventListener("sl-change", (event) => {
    if (!isValidIPv4(event.target.value)) {
      ipInput.setCustomValidity("Please enter a valid IP address.");
    } else {
      ipInput.setCustomValidity("");
    }
    ipInput.reportValidity();
  });

  function isValidIPv4(value) {
    if (value === "localhost") return true;
    const ipRegex =
      /^((25[0-5]|2[0-4][0-9]|1\d{2}|[1-9]?\d)(\.)){3}(25[0-5]|2[0-4][0-9]|1\d{2}|[1-9]?\d)$/;
    return ipRegex.test(value);
  }

  function fetchAndPopulateSelect(url, selectElement, onChangeCallback) {
    fetch(url)
      .then((res) => res.json())
      .then((items) => {
        selectElement.innerHTML = "";
        selectElement.disabled = false;
        items.forEach((item) => {
          const option = document.createElement("sl-option");
          option.value = item.id;
          option.textContent = item.name;
          selectElement.appendChild(option);
        });

        if (onChangeCallback) {
          selectElement.addEventListener("sl-change", (event) => {
            const selectedValue = event.target.value;
            onChangeCallback(selectedValue);
          });
        }
      })
      .catch((error) => {
        console.error(`Failed to fetch data from ${url}:`, error);
      });
  }

  function handleFetchVendors() {
    fetchAndPopulateSelect(
      "/datasources/modbus/vendors",
      vendorSelect,
      (vendorId) => handleFetchModels(vendorId)
    );
  }

  function handleFetchModels(vendorId) {
    fetchAndPopulateSelect(
      `/datasources/modbus/vendors/${vendorId}/models`,
      modelSelect,
      (modelId) => handleFetchDevices(modelId)
    );
  }

  function handleFetchDevices(modelId) {
    fetchAndPopulateSelect(
      `/datasources/modbus/models/${modelId}/devices`,
      deviceSelect,
      null
    );
  }

  handleFetchVendors();
}

document
  .getElementById("add-data-source-form")
  .addEventListener("submit", (event) => {
    event.preventDefault();

    const formData = new FormData(event.target);
    const dataSourceType = formData.get("dataSourceType");

    const newDataSource = {
      name: formData.get("name"),
      countryCode: formData.get("countryCode"),
      enabled: formData.get("enabled") === "on",
      asset: document.getElementById("asset-type").value,
      dataSourceType: document.getElementById("data-source-type").value,
    };

    if (dataSourceType === "SIMULATION") {
      newDataSource.simulationPeriod = parseInt(
        formData.get("simulationPeriod"),
        10
      );
    } else if (dataSourceType === "MODBUS") {
      newDataSource.modbusSettings = {
        modbusIp: formData.get("modbusIp"),
        modbusVendor: formData.get("modbusVendor"),
        modbusModel: formData.get("modbusModel"),
        modbusDevice: formData.get("modbusDevice"),
      };
    }

    fetch(DATASOURCES_BASE_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        [getCsrfHeader()]: getCsrfToken(),
      },
      body: JSON.stringify(newDataSource),
    })
      .then(async (response) => {
        if (!response.ok) {
          const errorMessage = await response.text();
          throw new Error(
            `Failed to add data source: [${response.status}] ${errorMessage}`
          );
        }
        return response.json();
      })
      .then((dataSourceSecrets) => {
        openSecretsDataSourceDialog(dataSourceSecrets);
      })
      .catch((error) => {
        console.error("Failed to add data source:", error);
        alert(`Failed to add data source: ${error.message}`);
      });
  });

document
  .getElementById("edit-data-source-form")
  .addEventListener("submit", (event) => {
    event.preventDefault();

    const formData = new FormData(event.target);
    const dataSourceId = event.target.getAttribute("data-id");

    const updatedDataSource = {
      id: dataSourceId,
      name: formData.get("name"),
      countryCode: formData.get("countryCode"),
      enabled: formData.get("enabled") === "on",
      asset: document.getElementById("asset-select").value,
      dataSourceType: formData.get("dataSourceType"),
    };

    if (formData.has("simulationPeriod")) {
      updatedDataSource.simulationPeriod = parseInt(
        formData.get("simulationPeriod"),
        10
      );
    }

    if (formData.has("modbusIp")) {
      updatedDataSource.modbusSettings = {
        modbusIp: formData.get("modbusIp"),
      };
    }

    fetch(`${DATASOURCES_BASE_URL}/${dataSourceId}`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        [getCsrfHeader()]: getCsrfToken(),
      },
      body: JSON.stringify(updatedDataSource),
    })
      .then(async (response) => {
        if (!response.ok) {
          const errorMessage = await response.text();
          throw new Error(
            `Failed to update data source: [${response.status}] ${errorMessage}`
          );
        }
      })
      .then(() => {
        closeEditDataSourceDialog();
        renderDataSources(); // Refresh the list after editing
      })
      .catch((error) => {
        console.error("Failed to update data source:", error);
      });
  });

// wait for Shoelace elements to ensure validation before submit
Promise.all([
  customElements.whenDefined("sl-button"),
  customElements.whenDefined("sl-input"),
]).then(() => {
  document
    .getElementById("permission-form")
    .addEventListener("submit", handlePermissionFormSubmit);
});

// process QR code scanner results
document
  .querySelector("qr-code-scanner")
  .addEventListener("result", (event) => {
    aiidaCodeInput.value = event.detail.result;
    aiidaCodeInput.updateComplete.then(() => {
      permissionForm.requestSubmit();
    });
  });

renderPermissions();
renderDataSources();

window.openRevokePermissionDialog = openRevokePermissionDialog;
window.updatePermission = updatePermission;
window.hidePermissionDialog = () => permissionDialog.hide();
window.hideRevokeDialog = () => revokeDialog.hide();
window.showUserDrawer = () => userDrawer.show();
window.hideUserDrawer = () => userDrawer.hide();
window.openAddDataSourceDialog = openAddDataSourceDialog;
window.closeAddDataSourceDialog = closeAddDataSourceDialog;
window.closeEditDataSourceDialog = closeEditDataSourceDialog;
window.closeSecretsDataSourceDialog = closeSecretsDataSourceDialog;
window.regenerateSecretsDataSource = regenerateSecretsDataSource;
window.downloadTlsCertificate = downloadTlsCertificate;

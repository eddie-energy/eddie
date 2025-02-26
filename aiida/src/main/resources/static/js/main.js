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

  addPermission();
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
    // check if input can be parsed into correct format
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
  console.log(permission);
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

      <dl class="permission-details">
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
      console.log("element: ", element);

      if (STATUS[permission.status].isActive) {
        activePermissionsList.insertAdjacentHTML("beforeend", element);
      } else {
        expiredPermissionsList.insertAdjacentHTML("beforeend", element);
      }
    });
  });
}

function addPermission() {
  const body = JSON.parse(atob(aiidaCodeInput.value));

  fetch(PERMISSIONS_BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
    body: JSON.stringify(body),
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

  fetch(`${PERMISSIONS_BASE_URL}/${permissionId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
    body: JSON.stringify({
      operation: operation,
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
      <dd>${schemas.map((schema) => `<span>${schema}</span>`).join("<br>")}</dd>

      <dt>Asset</dt>
      <dd>${asset}</dd>

      ${dataTags && dataTags.length
        ? `
            <dt>OBIS-Codes</dt>
            <dd>${dataTags.map((code) => `<span>${code}</span>`).join("<br>")}</dd>
        `
        : ""}
    </dl>

    <p class="text">
      <em>${serviceName}</em> requests permission to retrieve the near-realtime
      data for the given time frame and OBIS-codes. Please confirm the request
      is correct before granting permission.
    </p>
  `;

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
        const generalDetails = /* HTML */ ` <p>
            <strong>ID:</strong> ${dataSource.id}
          </p>
          <p><strong>Asset:</strong> ${dataSource.asset}</p>
          <p><strong>Type:</strong> ${dataSource.dataSourceType}</p>`;

        let dataSourceTypeDetails =
          dataSource.dataSourceType === "SIMULATION"
            ? /* HTML */ `
                <p>
                  <strong>Simulation Period:</strong>
                  ${dataSource.simulationPeriod}
                </p>
              `
            : /* HTML */ `
                <p>
                  <strong>MQTT Server URI:</strong> ${dataSource.mqttServerUri}
                </p>
                <p>
                  <strong>MQTT Topic:</strong> ${dataSource.mqttSubscribeTopic}
                </p>
                <p>
                  <strong>MQTT Username:</strong> ${dataSource.mqttUsername}
                </p>
                <p>
                  <strong>MQTT Password:</strong>
                  <span>
                    <span hidden id="mqtt-password">${dataSource.mqttPassword}</span>
                    <span>********</span>
                    <sl-icon
                      id="toggle-mqtt-password"
                      style="cursor: pointer"
                      name="eye"
                    ></sl-icon>
                  </span>
                </p>
              `;

        if (dataSource.dataSourceType === "Micro Teleinfo v3") {
          dataSourceTypeDetails += `<p><strong>Metering ID:</strong> ${dataSource.meteringId}</p>`;
        }

        template.innerHTML = /* HTML */ `
          <sl-card>
            <h3>${dataSource.name}</h3>
            ${generalDetails + dataSourceTypeDetails}
            <p>
              <strong>Enabled:</strong>
              <sl-switch
                class="toggle-enabled"
                ${dataSource.enabled ? "checked" : ""}
                data-id="${dataSource.id}"
              >
              </sl-switch>
            </p>
            <sl-button class="delete-button" data-id="${dataSource.id}">
              Delete
            </sl-button>
            <sl-button class="edit-button" data-id="${dataSource.id}">
              Edit
            </sl-button>
          </sl-card>
        `;

        dataSourceList.appendChild(template.content);
      });

      const passwordSpan = document.getElementById("mqtt-password");
      const toggleIcon = document.getElementById("toggle-mqtt-password");

      toggleIcon.addEventListener("click", () => {
        passwordSpan.toggleAttribute("hidden");
        if (passwordSpan.hasAttribute("hidden")) {
          toggleIcon.setAttribute("name", "eye");
        } else {
          toggleIcon.setAttribute("name", "eye-slash");
        }
      });

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
  const dialog = document.getElementById("add-data-source-dialog");
  const dataSourceSelect = document.getElementById("data-source-type");
  const assetSelect = document.getElementById("asset-type");

  fetch(`${DATASOURCES_BASE_URL}/types`)
    .then((response) => response.json())
    .then((types) => {
      console.log(types);
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
    .then((assets) => {
      assetSelect.innerHTML = assets
        .map(
          (asset) =>
            `<sl-option value="${asset.asset}">${asset.asset}</sl-option>`
        )
        .join("");

      if (assets.length > 0) {
        assetSelect.value = assets[0].asset;
      }
    })
    .catch((error) => console.error("Failed to fetch assets:", error));

  dialog.show();
}

function updateDataSourceFields(type) {
  const dataSourceFields = document.getElementById("data-source-fields");
  const commonFields = /* HTML */ `
    <sl-input name="name" label="Name" required></sl-input>
    <br />
    <sl-checkbox name="enabled" checked>Enabled</sl-checkbox>
    <br />
  `;

  let dataTypeFields = "";
  if (type === "SIMULATION") {
    dataTypeFields += `<br /><sl-input name="simulationPeriod" label="Simulation Period" type="number" required></sl-input>`;
  } else {
    dataTypeFields += `<br /><sl-input name="mqttTopic" label="MQTT Topic" required></sl-input>`;

    if (type === "MICRO_TELEINFO") {
      dataTypeFields += `<br /><sl-input name="meteringID" label="MeteringID" required></sl-input>`;
    }
  }

  dataSourceFields.innerHTML = commonFields + dataTypeFields;
}

function closeAddDataSourceDialog() {
  document.getElementById("add-data-source-dialog").hide();
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

      console.log(dataSource);
      Promise.all([
        fetch(`${DATASOURCES_BASE_URL}/types`).then((response) =>
          response.json()
        ),
        fetch(`${DATASOURCES_BASE_URL}/assets`).then((response) =>
          response.json()
        ),
      ])
        .then(([types, assets]) => {
          console.log(types);

          let editFields = /* HTML */ `
            <sl-input
              name="name"
              label="Name"
              value="${dataSource.name}"
              required
            ></sl-input>
            <br />
            <sl-checkbox name="enabled" ${dataSource.enabled ? "checked" : ""}>
              Enabled
            </sl-checkbox>
            <br />
            <br />
            <sl-select id="asset-select" name="asset" label="Asset" required>
              ${assets
                .map(
                  (asset) =>
                    `<sl-option value="${asset.asset}">${asset.asset}</sl-option>`
                )
                .join("")}
            </sl-select>
            <br />
            <sl-select
              id="type-select"
              name="dataSourceType"
              label="Type"
              required
            >
              ${types
                .map(
                  (type) =>
                    `<sl-option value="${type.identifier}">${type.name}</sl-option>`
                )
                .join("")}
            </sl-select>
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
          } else {
            editFields += /* HTML */ `
              <br />
              <sl-input
                name="mqttServerUri"
                label="MQTT Server URI"
                value="${dataSource.mqttServerUri}"
                required
              ></sl-input>
              <br />
              <sl-input
                name="mqttTopic"
                label="MQTT Topic"
                value="${dataSource.mqttSubscribeTopic}"
                required
              ></sl-input>
              <br />
              <sl-input
                name="mqttUsername"
                label="MQTT Username"
                value="${dataSource.mqttUsername}"
                required
              ></sl-input>
              <br />
              <sl-input
                name="mqttPassword"
                label="MQTT Password"
                value="${dataSource.mqttPassword}"
                required
                type="password"
                password-toggle
              ></sl-input>
            `;

            if (dataSource.dataSourceType === "TELEINFO") {
              editFields += /* HTML */ `
                <br />
                <sl-input
                  name="meteringId"
                  label="Metering ID"
                  value="${dataSource.meteringId}"
                  required
                ></sl-input>
              `;
            }
          }

          editDataSourceFields.innerHTML = editFields;

          const assetSelect = document.getElementById("asset-select");
          const typeSelect = document.getElementById("type-select");

          assetSelect.value = dataSource.asset;
          typeSelect.value = dataSource.dataSourceType;

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

document
  .getElementById("add-data-source-form")
  .addEventListener("submit", (event) => {
    event.preventDefault();

    const formData = new FormData(event.target);
    const dataSourceType = formData.get("dataSourceType");

    const newDataSource = {
      name: formData.get("name"),
      enabled: formData.get("enabled") === "on",
      asset: document.getElementById("asset-type").value,
      dataSourceType: document.getElementById("data-source-type").value,
    };

    if (dataSourceType === "SIMULATION") {
      newDataSource.simulationPeriod = parseInt(
        formData.get("simulationPeriod"),
        10
      );
    } else {
      newDataSource.mqttSubscribeTopic = formData.get("mqttTopic");

      if (dataSourceType === "MICRO_TELEINFO") {
        newDataSource.meteringId = formData.get("meteringID");
      }
    }

    console.log(newDataSource);

    fetch(DATASOURCES_BASE_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        [getCsrfHeader()]: getCsrfToken(),
      },
      body: JSON.stringify(newDataSource),
    })
      .then((response) => {
        if (!response.ok) throw new Error("Failed to add data source");
        return response;
      })
      .then(() => {
        closeAddDataSourceDialog();
        renderDataSources();
      })
      .catch((error) => console.error("Failed to add data source:", error));
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
      enabled: formData.get("enabled") === "on",
      asset: document.getElementById("asset-select").value,
      dataSourceType: document.getElementById("type-select").value,
      mqttServerUri: formData.get("mqttServerUri"),
      mqttSubscribeTopic: formData.get("mqttTopic"),
      mqttUsername: formData.get("mqttUsername"),
      mqttPassword: formData.get("mqttPassword"),
      meteringId: formData.get("meteringID"),
    };
    console.log(updatedDataSource);

    if (formData.has("simulationPeriod")) {
      updatedDataSource.simulationPeriod = parseInt(
        formData.get("simulationPeriod"),
        10
      );
    }

    fetch(`${DATASOURCES_BASE_URL}/${dataSourceId}`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        [getCsrfHeader()]: getCsrfToken(),
      },
      body: JSON.stringify(updatedDataSource),
    })
      .then((response) => {
        if (!response.ok) throw new Error("Failed to update data source");
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

renderPermissions();
renderDataSources();

window.openRevokePermissionDialog = openRevokePermissionDialog;
window.addPermission = addPermission;
window.updatePermission = updatePermission;
window.hidePermissionDialog = () => permissionDialog.hide();
window.hideRevokeDialog = () => revokeDialog.hide();
window.showUserDrawer = () => userDrawer.show();
window.hideUserDrawer = () => userDrawer.hide();
window.openAddDataSourceDialog = openAddDataSourceDialog;
window.closeAddDataSourceDialog = closeAddDataSourceDialog;
window.closeEditDataSourceDialog = closeEditDataSourceDialog;

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

const BASE_URL = "/permissions";

const permissionDialog = document.getElementById("permission-dialog");
const permissionDialogContent = document.getElementById(
  "permission-dialog-content",
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
  return document.querySelector('meta[name="csrf-header"]').getAttribute('content');
}

function getCsrfToken() {
  return document.querySelector('meta[name="csrf-token"]').getAttribute('content');
}

function permissions() {
  return fetch(BASE_URL).then((response) => response.json());
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
    const { permissionId, serviceName, handshakeUrl, accessToken } = JSON.parse(
      atob(aiidaCodeInput.value),
    );
  } catch (error) {
    console.debug(error);
    aiidaCodeInput.setCustomValidity(
      "Please confirm you entered a valid AIIDA code",
    );
  }
});

function toLocalDateString(time) {
  return new Date(time).toLocaleString();
}

function permissionElement(permission) {
  console.log(permission)
  const { permissionId, status, serviceName } = permission;
  const dataTags = permission.hasOwnProperty("dataNeed")
    ? permission.dataNeed.dataTags
    : ["Not available yet."];
  const startTime = permission.hasOwnProperty("startTime")
    ? toLocalDateString(permission.startTime)
    : "Not available yet.";
  const expirationTime = permission.hasOwnProperty("expirationTime")
    ? toLocalDateString(permission.expirationTime)
    : "Not available yet.";
  const transmissionSchedule = permission.hasOwnProperty("dataNeed")
    ? permission.dataNeed.transmissionSchedule
    : "Not available yet.";
  const asset = permission.hasOwnProperty("dataNeed")
    ? permission.dataNeed.asset
    : "Not available yet.";
  const schemas = permission.hasOwnProperty("dataNeed")
    ? permission.dataNeed.schemas
    : ["Not available yet."];

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

        <dt>Permission ID</dt>
        <dd>${permissionId}</dd>

        <dt>Start</dt>
        <dd>${startTime}</dd>

        <dt>End</dt>
        <dd>${expirationTime}</dd>

        <dt>Transmission Schedule</dt>
        <dd>${transmissionSchedule}</dd>

        <dt>Schemas</dt>
        <dd>
          ${schemas.map((schema) => `<span>${schema}</span>`).join("<br>")}
        </dd>

        <dt>Asset</dt>
        <dd>${asset}</dd>

        <dt>OBIS-Codes</dt>
        <dd>${dataTags.map((code) => `<span>${code}</span>`).join("<br>")}</dd>
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

function addPermission() {
  const body = JSON.parse(atob(aiidaCodeInput.value));

  fetch(BASE_URL, {
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
            `Failed to add permission: ${json.errors[0].message}`,
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
          </sl-alert>`,
      );

      acceptButton.loading = rejectButton.loading = closeButton.loading = false;
      closeButton.disabled = false;
    });
}

function updatePermission(operation) {
  console.debug(operation + " permission");

  const { permissionId } = JSON.parse(atob(aiidaCodeInput.value));

  fetch(`${BASE_URL}/${permissionId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    },
    body: JSON.stringify({
      operation: operation,
    }),
  }).then((res) => {
    permissionDialog.hide();
    aiidaCodeInput.value = "";
    renderPermissions();
  });
}

function updatePermissionDialogWithDetails(permission) {
  console.debug("Updating dialog with details", permission);

  const {
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

      <dt>Permission ID</dt>
      <dd>${permissionId}</dd>

      <dt>Start</dt>
      <dd>${toLocalDateString(startTime)}</dd>

      <dt>End</dt>
      <dd>${toLocalDateString(expirationTime)}</dd>

      <dt>Transmission Schedule</dt>
      <dd>${transmissionSchedule}</dd>

      <dt>Schemas</dt>
      <dd>${schemas.map((schema) => `<span>${schema}</span>`).join("<br>")}</dd>

      <dt>Asset</dt>
      <dd>${asset}</dd>

      <dt>OBIS-Codes</dt>
      <dd>${dataTags.map((code) => `<span>${code}</span>`).join("<br>")}</dd>
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
  fetch(`${BASE_URL}/${permissionId}`, {
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

function logout() {
  fetch("/logout", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [getCsrfHeader()]: getCsrfToken(),
    }
  }).then(() => {
    window.location.href = "/";
  });
}

function openAccountSettings() {
  window.location.href = "/account";
}

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

window.openRevokePermissionDialog = openRevokePermissionDialog;
window.addPermission = addPermission;
window.updatePermission = updatePermission;
window.hidePermissionDialog = () => permissionDialog.hide();
window.hideRevokeDialog = () => revokeDialog.hide();
window.showUserDrawer = () => userDrawer.show();
window.hideUserDrawer = () => userDrawer.hide();
window.openAccountSettings = openAccountSettings;
window.logout = logout;

const STATUS = {
  ACCEPTED: {
    title: "Accepted",
    description:
      "The permission request was accepted by the user and is being processed.",
    isActive: true,
    isRevocable: true,
  },
  WAITING_FOR_START: {
    title: "Waiting for Start",
    description:
      "The permission request was accepted and is scheduled to start at the specified start time.",
    isActive: true,
    isRevocable: true,
  },
  STREAMING_DATA: {
    title: "Streaming Data",
    description:
      "The permission request was accepted and data is now actively streamed to the eligible party.",
    isActive: true,
    isRevocable: true,
  },
  REJECTED: {
    title: "Rejected",
    description: "The user rejected the permission request.",
  },
  REVOCATION_RECEIVED: {
    title: "Revocation Received",
    description: "The user requested revocation of the permission.",
    isActive: true,
  },
  REVOKED: {
    title: "Revoked",
    description: "The user revoked the permission.",
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
};

const { VITE_API_BASE_URL: BASE_URL } = import.meta.env;

const permissionDialog = document.getElementById("permission-dialog");
const permissionDialogContent = document.getElementById(
  "permission-dialog-content",
);

const revokeDialog = document.getElementById("revoke-permission-dialog");
const revokeButton = document.getElementById("revoke-permission-button");

const aiidaCodeInput = document.getElementById("aiida-code");

const activePermissionsList = document.getElementById("active-permissions");
const expiredPermissionsList = document.getElementById("expired-permissions");

function permissions() {
  return fetch(BASE_URL).then((response) => response.json());
}

function handlePermissionFormSubmit(event) {
  event.preventDefault();

  const permission = JSON.parse(atob(aiidaCodeInput.value));

  console.log(permission);

  const {
    serviceName,
    connectionId,
    permissionId,
    startTime,
    expirationTime,
    requestedCodes,
  } = permission;

  permissionDialogContent.innerHTML = /* HTML */ `
    <span>Permission request from</span>

    <h3>
      <strong>${serviceName}</strong>
    </h3>

    <dl class="permission-details">
      <dt>Service</dt>
      <dd>${serviceName}</dd>

      <dt>Connection ID</dt>
      <dd>${connectionId}</dd>

      <dt>Permission ID</dt>
      <dd>${permissionId}</dd>

      <dt>Start</dt>
      <dd>${new Date(startTime).toLocaleDateString()}</dd>

      <dt>End</dt>
      <dd>${new Date(expirationTime).toLocaleDateString()}</dd>

      <dt>OBIS-Codes</dt>
      <dd>
        ${requestedCodes.map((code) => `<span>${code}</span>`).join("<br>")}
      </dd>
    </dl>

    <p class="text">
      <em>${serviceName}</em> requests permission to retrieve the near-realtime
      data for the given time frame and OBIS-codes. Please confirm the request
      is correct before granting permission.
    </p>
  `;
  permissionDialog.show();
}

aiidaCodeInput.addEventListener("sl-input", () => {
  try {
    // check if input can be parsed into correct format
    // noinspection JSUnusedLocalSymbols
    const {
      permissionId,
      serviceName,
      startTime,
      expirationTime,
      connectionId,
      requestedCodes,
    } = JSON.parse(atob(aiidaCodeInput.value));
  } catch (error) {
    console.debug(error);
    aiidaCodeInput.setCustomValidity(
      "Please confirm you entered a valid AIIDA code",
    );
  }
});

function permissionElement(permission) {
  const {
    permissionId,
    status,
    serviceName,
    startTime,
    expirationTime,
    connectionId,
    requestedCodes,
  } = permission;

  return /* HTML */ `
    <sl-details>
      <span slot="summary">
        <strong>${serviceName}</strong><br />
        <small class="label">${permissionId} / ${connectionId}</small>
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

        <dt>Connection ID</dt>
        <dd>${connectionId}</dd>

        <dt>Permission ID</dt>
        <dd>${permissionId}</dd>

        <dt>Start</dt>
        <dd>${new Date(startTime * 1000).toLocaleDateString()}</dd>

        <dt>End</dt>
        <dd>${new Date(expirationTime * 1000).toLocaleDateString()}</dd>

        <dt>OBIS-Codes</dt>
        <dd>
          ${requestedCodes.map((code) => `<span>${code}</span>`).join("<br>")}
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

function addPermission() {
  const body = JSON.parse(atob(aiidaCodeInput.value));
  body.grantTime = new Date().toISOString();

  fetch(BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("Failed to add permission");
      }
      return response.json();
    })
    .then(() => {
      permissionDialog.hide();
      aiidaCodeInput.value = "";
      renderPermissions();
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
    });
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
    },
    body: JSON.stringify({
      operation: "REVOKE",
    }),
  }).then(() => renderPermissions());
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
window.hidePermissionDialog = () => permissionDialog.hide();
window.hideRevokeDialog = () => revokeDialog.hide();

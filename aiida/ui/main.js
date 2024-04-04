const BASE_URL = "http://localhost:8081/permissions";

const STATUS_ACTIVE = [
  "ACCEPTED",
  "WAITING_FOR_START",
  "STREAMING_DATA",
  "REVOCATION_RECEIVED",
];
const STATUS_EXPIRED = ["REJECTED", "REVOKED", "TERMINATED", "TIME_LIMIT"];
const STATUS_REVOCABLE = ["ACCEPTED", "WAITING_FOR_START", "STREAMING_DATA"];

const permissionDialog = document.querySelector(".js-permission-dialog");
const permissionDialogContent = document.querySelector(
  ".js-permission-dialog-content",
);

const aiidaCodeInput = document.getElementById("aiida-code");

const activePermissionsList = document.getElementById("active-permissions");
const expiredPermissionsList = document.getElementById("expired-permissions");

const changeEvent = new Event("permissions-changed");

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
      status,
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

function renderPermissions() {
  permissions().then((permissions) => {
    activePermissionsList.innerHTML = "";
    expiredPermissionsList.innerHTML = "";

    permissions.forEach((permission) => {
      const {
        permissionId,
        status,
        serviceName,
        startTime,
        expirationTime,
        connectionId,
        requestedCodes,
      } = permission;

      const element = /* HTML */ `
        <sl-details open>
          <span slot="summary">
            <strong>${serviceName}</strong><br />
            <small class="label">${permissionId} / ${connectionId}</small>
          </span>

          <dl class="permission-details">
            <dt>Service</dt>
            <dd>${serviceName}</dd>
            <dt>Status</dt>
            <dd>
              <sl-badge
                variant="${STATUS_ACTIVE.includes(status)
                  ? "success"
                  : "danger"}"
                >${status}</sl-badge
              >
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
              ${requestedCodes
                .map((code) => `<span>${code}</span>`)
                .join("<br>")}
            </dd>
          </dl>
          ${STATUS_REVOCABLE.includes(status)
            ? `
                <sl-button class="js-revoke-permission" data-permission-id="${permissionId}" style="margin-top: 1rem">
                  Revoke
                </sl-button>
              `
            : ""}
        </sl-details>
      `;

      if (STATUS_ACTIVE.includes(status)) {
        activePermissionsList.insertAdjacentHTML("beforeend", element);
      } else if (STATUS_EXPIRED.includes(status)) {
        expiredPermissionsList.insertAdjacentHTML("beforeend", element);
      }
    });

    document.querySelectorAll(".js-revoke-permission").forEach((button) => {
      button.addEventListener("click", () => {
        revokePermission(button.dataset.permissionId);
      });
    });
  });
}

function addPermission(aiidaCode) {
  const body = JSON.parse(atob(aiidaCode));
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
      document.dispatchEvent(changeEvent);
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

function revokePermission(permissionId) {
  fetch(`${BASE_URL}/${permissionId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      operation: "REVOKE_PERMISSION",
    }),
  }).then(() => document.dispatchEvent(changeEvent));
}

document.addEventListener("permissions-changed", renderPermissions);

document
  .querySelector(".js-add-permission")
  .addEventListener("click", () => addPermission(aiidaCodeInput.value));

document
  .querySelector(".js-permission-dialog-hide")
  .addEventListener("click", () => permissionDialog.hide());

// wait for Shoelace elements to ensure validation before submit
Promise.all([
  customElements.whenDefined("sl-button"),
  customElements.whenDefined("sl-input"),
]).then(() => {
  document
    .querySelector(".js-permission-form")
    .addEventListener("submit", handlePermissionFormSubmit);
});

renderPermissions();

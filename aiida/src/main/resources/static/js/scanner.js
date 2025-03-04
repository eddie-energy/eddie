const aiidaCodeInput = document.querySelector("#aiida-code");
const permissionForm = document.querySelector("#permission-form");
const scanQrCodeButton = document.querySelector("#scan-qr-code");
const scanQrCodeDialog = document.querySelector("#scan-qr-code-dialog");
const scanQrCodeSelect = document.querySelector("#scan-qr-code-select");
const scanQrCodeVideo = document.querySelector("#scan-qr-code-video");
const scanQrCodeLoading = document.querySelector("#scan-qr-code-loading");

try {
  const codeReader = new ZXing.BrowserQRCodeReader();
  const videoInputDevices = await codeReader.getVideoInputDevices();

  let selectedDeviceId = videoInputDevices[0].deviceId;
  if (videoInputDevices.length >= 1) {
    for (const { deviceId, label } of videoInputDevices) {
      const sourceOption = document.createElement("sl-option");
      sourceOption.textContent = label;
      sourceOption.value = deviceId;
      scanQrCodeSelect.appendChild(sourceOption);
    }

    scanQrCodeSelect.addEventListener("sl-change", () => {
      selectedDeviceId = scanQrCodeSelect.value;
      codeReader.reset();
      decode(codeReader, selectedDeviceId);
    });
  }

  scanQrCodeButton.addEventListener("click", () => {
    scanQrCodeDialog.show();
    decode(codeReader, selectedDeviceId);
  });
} catch (error) {
  console.debug(error);
  scanQrCodeButton.toggleAttribute("disabled");
}

function decode(codeReader, deviceId) {
  codeReader
    .decodeFromInputVideoDevice(deviceId, scanQrCodeVideo)
    .then((result) => {
      console.debug(result);
      aiidaCodeInput.value = btoa(result);
      scanQrCodeDialog.hide();
    })
    .catch((err) => {
      console.error(err);
    });
}

// Hide video until it is showing content
scanQrCodeVideo.addEventListener("play", () => {
  scanQrCodeVideo.removeAttribute("hidden");
  scanQrCodeLoading.remove();
});

window.hideScanQrCodeDialog = () => {
  scanQrCodeDialog.hide();
};

const aiidaCodeInput = document.querySelector("#aiida-code");
const scanQrCodeButton = document.querySelector("#scan-qr-code");
const scanQrCodeClose = document.querySelector("#scan-qr-code-close");
const scanQrCodeDialog = document.querySelector("#scan-qr-code-dialog");
const scanQrCodeVideo = document.querySelector("#scan-qr-code-video");
const scanQrCodeLoading = document.querySelector("#scan-qr-code-loading");
const scanQrCodeError = document.querySelector("#scan-qr-code-error");

const codeReader = new ZXingBrowser.BrowserQRCodeReader();

scanQrCodeButton.addEventListener("click", async () => {
  scanQrCodeDialog.show();

  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "environment" },
    });

    const result = await codeReader.decodeOnceFromStream(
      stream,
      scanQrCodeVideo
    );

    aiidaCodeInput.value = btoa(result);
    scanQrCodeDialog.hide();
  } catch (error) {
    console.error(error);
    scanQrCodeLoading.remove();
    scanQrCodeError.innerHTML = /* HTML */ `
      <sl-alert variant="danger" open>
        <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
        ${error}
      </sl-alert>
    `;
  }
});

// Hide video until it is showing content
scanQrCodeVideo.addEventListener("play", () => {
  scanQrCodeVideo.removeAttribute("hidden");
  scanQrCodeLoading.remove();
});

scanQrCodeClose.addEventListener("click", () => {
  codeReader.reset();
  scanQrCodeDialog.hide();
});

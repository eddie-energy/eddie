import { BrowserQRCodeReader } from "https://esm.sh/@zxing/browser@0.1.5";

class QrCodeScanner extends HTMLElement {
  #codeReader = new BrowserQRCodeReader();
  #dialog;
  #video;
  #loading;
  #error;

  constructor() {
    super();
    this.attachShadow({ mode: "open" });
  }

  connectedCallback() {
    this.render();

    this.#dialog = this.shadowRoot.querySelector("#dialog");
    this.#video = this.shadowRoot.querySelector("#video");
    this.#loading = this.shadowRoot.querySelector("#loading");
    this.#error = this.shadowRoot.querySelector("#error");
    this.setupEventListeners();
  }

  render() {
    this.shadowRoot.innerHTML = /* HTML */ `
      <style>
        :host {
          display: inline-block;
        }

        video {
          width: 100%;
        }
      </style>

      <sl-button id="scan" circle>
        <sl-icon name="camera" label="Camera"></sl-icon>
      </sl-button>

      <sl-dialog id="dialog" label="Scan AIIDA code">
        <video id="video" hidden></video>
        <div id="loading">
          Waiting for camera...
          <br />
          <sl-spinner></sl-spinner>
        </div>

        <div id="error"></div>

        <sl-button id="close" slot="footer" outline>Close</sl-button>
      </sl-dialog>
    `;
  }

  setupEventListeners() {
    this.shadowRoot
      .querySelector("#scan")
      .addEventListener("click", this.startScanning.bind(this));

    this.shadowRoot
      .querySelector("#close")
      .addEventListener("click", this.stopScanning.bind(this));

    // Hide video until it starts playing
    this.#video.addEventListener("play", () => {
      this.#video.removeAttribute("hidden");
      this.#loading.hidden = true;
    });
  }

  async startScanning() {
    // Reset previous state
    this.#error.innerHTML = "";

    this.#dialog.show();

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
      });

      const result = await this.#codeReader.decodeOnceFromStream(
        stream,
        this.#video
      );

      const event = new CustomEvent("result", {
        detail: {
          result,
        },
      });
      this.dispatchEvent(event);

      this.#dialog.hide();
    } catch (error) {
      console.error(error);

      // Remove loading indicator
      this.#loading.hidden = true;

      this.#error.innerHTML = /* HTML */ `
        <sl-alert variant="danger" open>
          <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
          ${error.message || error}
        </sl-alert>
      `;
    }
  }

  stopScanning() {
    this.#codeReader.reset();
    this.#dialog.hide();
  }
}

customElements.define("qr-code-scanner", QrCodeScanner);

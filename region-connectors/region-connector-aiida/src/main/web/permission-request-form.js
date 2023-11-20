import { css, html, LitElement } from "lit";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/qr-code/qr-code.js";

const BASE_URL = new URL(import.meta.url).href.replace("ce.js", "");
const REQUEST_URL = BASE_URL + "permission-request";

class PermissionRequestForm extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
  };

  static styles = css`
    .hidden {
      display: none;
    }
  `;

  constructor() {
    super();
    // create a new shared stylesheet
    const sheet = new CSSStyleSheet();

    // add CSS styles
    sheet.replaceSync(`
    .toast {
   visibility: visible;
   min-width: 250px;
   margin-left: -125px;
   background-color: #333;
   color: #fff;
   text-align: center;
   border-radius: 25px; /* Add this line */
   padding: 16px;
   position: fixed;
   z-index: 1000;
   left: 50%;
   bottom: 30px;
   animation: fadein 0.5s, fadeout 0.5s 2.5s;
 }

 @-webkit-keyframes fadein {
   from {
     bottom: 0;
     opacity: 0;
   }
   to {
     bottom: 30px;
     opacity: 1;
   }
 }
 @keyframes fadein {
   from {
     bottom: 0;
     opacity: 0;
   }
   to {
     bottom: 30px;
     opacity: 1;
   }
 }
 @-webkit-keyframes fadeout {
   from {
     bottom: 30px;
     opacity: 1;
   }
   to {
     bottom: 0;
     opacity: 0;
   }
 }
 @keyframes fadeout {
   from {
     bottom: 30px;
     opacity: 1;
   }
   to {
     bottom: 0;
     opacity: 0;
   }
 }
`);

    // apply the stylesheet to a document
    document.adoptedStyleSheets = [sheet];
  }

  requestPermission(event) {
    let body = {
      dataNeedId: this.dataNeedAttributes.id,
      connectionId: this.connectionId,
    };

    fetch(REQUEST_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })
      .then((response) => response.json())
      .then((result) => {
        this.displayResponse(result);
      })
      .catch((error) => console.error(error));
  }

  displayResponse(json) {
    let jsonString = JSON.stringify(json);

    let qrCode = this.renderRoot.querySelector("sl-qr-code");
    qrCode.value = JSON.stringify(json);
    qrCode.style.display = "block";

    let base64String = btoa(jsonString);
    let input = this.renderRoot.querySelector("sl-input");
    input.value = base64String;
    input.style.display = "block";

    let connectButton = this.renderRoot.querySelector("sl-button");
    connectButton.style.display = "none";
  }

  copyToClipboard() {
    let input = this.renderRoot.querySelector("sl-input");
    input.select();
    let copyText = input.value;

    navigator.clipboard
      .writeText(copyText)
      .then(() => {
        this.showToast("Copied to clipboard");
      })
      .catch(function (err) {
        console.error("Failed to copy text: ", err);
      });
  }

  showToast(message) {
    let toast = document.createElement("div");
    toast.textContent = message;
    toast.classList.add("toast");
    document.body.appendChild(toast);
    setTimeout(function () {
      document.body.removeChild(toast);
    }, 3000);
  }

  render() {
    return html`
      <div>
        <h1>THIS IS AIIDA!!!</h1>

        <div>
          <sl-button @click="${this.requestPermission}" variant="primary"
            >Connect
          </sl-button>

          <br />

          <div class="qr-overview">
            <sl-qr-code
              class="hidden"
              value="https://shoelace.style/"
              radius="0.5"
              size="256"
            ></sl-qr-code>
          </div>

          <sl-input
            class="label-on-left hidden"
            label="Code"
            help-text="Enter this code on the website of your AIIDA device"
            size="medium"
            readonly
          >
            <sl-icon
              name="copy"
              slot="suffix"
              @click="${this.copyToClipboard}"
            ></sl-icon>
          </sl-input>

          <style>
            .label-on-left {
              --label-width: 3.75rem;
              --gap-width: 1rem;
            }

            .label-on-left + .label-on-left {
              margin-top: var(--sl-spacing-medium);
            }

            .label-on-left::part(form-control) {
              display: grid;
              grid: auto / var(--label-width) 1fr;
              gap: var(--sl-spacing-3x-small) var(--gap-width);
              align-items: center;
            }

            .label-on-left::part(form-control-label) {
              text-align: right;
            }

            .label-on-left::part(form-control-help-text) {
              grid-column-start: 2;
            }
          </style>
        </div>
      </div>
    `;
  }
}

export default PermissionRequestForm;

import { html, css, LitElement } from "lit";
import { ref, createRef } from "lit/directives/ref.js";

class EddieConnectButton extends LitElement {

    static properties = {
        connectionid: {},
        _availableConnectors: {},
    };

    static CONTEXT_PATH = (new URL(import.meta.url)).pathname.replace(/\/lib\/.*$/, "");

    dialogRef = createRef();
    connectorPlaceholderRef = createRef();

    static styles = css`
      button.ce {
        font-size: 12pt;
        background-color: #eee;
        border: 3pt solid #55f;
        border-radius: 1em;
        display: inline-block;
        min-height: 2.5em;
        padding: 0 1em;
      }

      dialog {
        background-color: #eee;
        border: 3pt solid white;
        border-radius: 1em;
        min-width: 50%;
        min-height: 50%;
        overflow-x: hidden;
      }

      dialog::backdrop {
        background-color: #00092288;
      }

      .close {
        display: block;
        border: none;
        background: none;
        position: absolute;
        right: 1ex;
        top: 1ex;
      }

      .close:hover {
        transform: scale(2);
      }
    `;

    constructor() {
        super();
        this._availableConnectors = [];
    }

    connect() {
        this.dialogRef.value.showModal();
        this.loadAvailablePas();
    }

    closePopup() {
        this.dialogRef.value.close();
    }

    loadAvailablePas() {
        const queryUrl = new URL(import.meta.url);
        queryUrl.pathname = EddieConnectButton.CONTEXT_PATH + "/api/region-connectors-metadata";
        fetch(queryUrl)
            .then(res => {
                if (res.ok) {
                    return res.json();
                } else {
                    console.error(`cannot query available PAs from ${queryUrl.href}`);
                    console.error(res);
                    return null;
                }
            }).then(json => this._availableConnectors = Object.fromEntries(json.map(it => [it.mdaCode, it])))
            .catch(err => console.error(err));
    }

    selectPa(event) {
        const paName = event.target.value;
        if (paName) {
            console.log(`selected pa ${paName}, loading it's custom element`);
            this.loadPaSpecificCustomElement(event.target.value).catch(console.error);
        } else {
            console.log("unselected the pa");
            this.connectorPlaceholderRef.value.innerHTML = "... no PA selected ...";
        }
    }

    async loadPaSpecificCustomElement(paName) {
        const customElementName = paName.toLocaleLowerCase() + "-pa-ce";
        if (typeof customElements.get(customElementName) === "undefined") {
            // loaded module needs to have the custom element class as it's default export
            const regionConnectorUrlPath = this._availableConnectors[paName].urlPath;
            const module = await import(`${EddieConnectButton.CONTEXT_PATH}${regionConnectorUrlPath}ce.js`);
            customElements.define(customElementName, module.default);
        }
        const el = document.createElement(customElementName);
        el.setAttribute("connectionid", this.connectionid)
        this.connectorPlaceholderRef.value.replaceChildren(el);
    }

    render() {
        return html`
            <button class="ce" @click=${this.connect}>Connect with EDDIE</button>
            <dialog ${ref(this.dialogRef)}>
                <button class="close" @click=${this.closePopup}>&times;</button>
                <label for="connector-select">Please select a PA:</label>
                <select id="connector-select" @change="${this.selectPa}">
                    <option value="">..please select</option>
                    ${Object.keys(this._availableConnectors)
                            .sort((a, b) => a.localeCompare(b))
                            .map(con => html`
                                <option value="${con}">${con}</option>`)}
                </select>
                <div ${ref(this.connectorPlaceholderRef)}>... no PA selected yet...</div>
            </dialog>
        `;
    }
}

export default EddieConnectButton;
import { css, html, LitElement } from "https://esm.sh/lit";
import { commonParameters } from "./common-parameters-form.js";

class SetConnectionStatusCe extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    _statusValues: {},
  };
  static styles = css``;

  constructor() {
    super();
    this._statusValues = [];
    fetch("connection-status-values")
      .then((res) => res.json())
      .then((json) => (this._statusValues = json))
      .catch(console.error);
  }

  submit() {
    const connectionStatus = this.renderRoot.querySelector(
      "input[name='connectionStatus']:checked"
    ).value;
    console.log(
      `switching connection status of ${this.connectionId} to ${connectionStatus}`
    );
    const body = JSON.stringify({
      connectionId: commonParameters.connectionId,
      dataNeedId: commonParameters.dataNeedId,
      permissionId: commonParameters.permissionId,
      connectionStatus: connectionStatus,
    });
    fetch("connection-status", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body,
    }).catch(console.error);
  }

  render() {
    return html`
      <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha3/dist/css/bootstrap.min.css"
      />
      <div class="card mb-3">
        <div class="card-header">Connection Status</div>
        <div class="card-body">
          <div class="row align-items-start">
            ${this._statusValues.map(
              (val, index) => html`
                <div class="col form-check">
                  <input
                    class="form-check-input"
                    type="radio"
                    name="connectionStatus"
                    id="connectionStatus${val}"
                    value="${val}"
                    .checked=${0 == index}
                  />
                  <label class="form-check-label" for="connectionStatus${val}"
                    >${val}</label
                  >
                </div>
              `
            )}
          </div>
          <div class="row mt-3 mb-3">
            <button class="col-3 btn btn-primary" @click="${this.submit}">
              Submit
            </button>
          </div>
        </div>
      </div>
    `;
  }
}

window.customElements.define("set-connection-status", SetConnectionStatusCe);
export default SetConnectionStatusCe;

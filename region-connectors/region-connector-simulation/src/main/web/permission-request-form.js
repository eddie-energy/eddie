import { css, html } from "https://esm.sh/lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/select/select.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";

const baseUrl = import.meta.url.substring(0, import.meta.url.lastIndexOf("/"));

class SimulationConnectorButtonCe extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    baseUrl: { attribute: "base-url" }, // might be beneficial to read this now separate from the form base
    _scenarios: { type: Array },
    _permissionId: { type: String },
  };

  static styles = css`
    dl {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 0.5rem;
    }

    dt {
      font-style: italic;
    }
  `;

  constructor() {
    super();
    this._scenarios = [];
    this._permissionId = crypto.randomUUID().toString();
  }

  connectedCallback() {
    super.connectedCallback();
    fetch(`${this.baseUrl}/scenarios`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    })
      .then((res) => res.json())
      .then((res) => (this._scenarios = res))
      .catch((error) => console.error(error));
  }

  async handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const response = await fetch(
      `${this.baseUrl}/scenarios/${formData.get("scenario").replaceAll("-", " ")}/run`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          connectionId: this.connectionId,
          permissionId: formData.get("permission-id"),
          dataNeedId: this.dataNeedId,
        }),
      }
    );
    const data = await response.json();
    if (response.ok) {
      this.notify({
        title: "Executing Scenario!",
        message: "Your scenario is currently being executed.",
        variant: "success",
        duration: 10000,
      });
      this.pollRequestStatus(`${this.requestStatusUrl}/${data.permissionId}`);
    } else {
      const { errors } = data;
      errors.forEach((err) => this.error(err.message));
    }
  }

  render() {
    const targetUrl = `${this.baseUrl}/simulation.html?connectionId=${this.connectionId}&dataNeedId=${this.dataNeedId}`;

    return html`
      <div>
        <h4>Simulation MDA</h4>
        <p>
          The following information about the data need of the EP application
          was given:
        </p>

        <dl>
          <dt>connectionId</dt>
          <dd>${this.connectionId}</dd>
          <dt>dataNeedId</dt>
          <dd>${this.dataNeedId}</dd>
        </dl>

        <sl-button href="${targetUrl}" target="_blank" variant="primary">
          Launch Simulation
        </sl-button>
        <form id="request-form">
          <h3>Run Scenario</h3>
          <div>
            <sl-input
              id="permission-id"
              name="permission-id"
              type="text"
              label="Permission ID"
              value="${this._permissionId}"
              filled
              required
            />
          </div>
          <br />
          <div>
            <sl-select id="scenario" name="scenario" label="Scenario" required>
              ${this._scenarios.map(
                (scenario) => html`
                  <sl-option value="${scenario.replaceAll(" ", "-")}">
                    ${scenario}
                  </sl-option>
                `
              )}
            </sl-select>
          </div>
          <br />
          <sl-button type="submit" variant="primary">Run Scenario</sl-button>
        </form>
      </div>
    `;
  }
}

export default SimulationConnectorButtonCe;

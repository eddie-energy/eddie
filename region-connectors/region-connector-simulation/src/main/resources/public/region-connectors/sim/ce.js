import { css, html, LitElement } from "https://esm.sh/lit";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/select/select.js";

const baseUrl = import.meta.url.substring(0, import.meta.url.lastIndexOf("/"));

class SimulationConnectorButtonCe extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    _scenarios: { type: Array },
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
  }

  connectedCallback() {
    super.connectedCallback();
    fetch(`${baseUrl}/scenarios`, {
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
    try {
      const response = await fetch(
        `${baseUrl}/scenarios/${formData.get("scenario").replaceAll("-", " ")}/run`,
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
        },
      );

      if (!response.ok) {
        throw new Error("Failed to send message");
      }

      const responseData = await response.json();
      console.log("Response:", responseData); // Log the response for debugging
    } catch (error) {
      console.error("Error:", error); // Log any error that occurs during the fetch
    }
  }

  render() {
    const targetUrl = `${baseUrl}/simulation.html?connectionId=${this.connectionId}&dataNeedId=${this.dataNeedId}`;

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
        <form id="run-scenario-form" @submit="${this.handleSubmit}">
          <h3>Run Scenario</h3>
          <div>
            <label for="permissionId">PermissionID:</label>
            <input id="permission-id" name="permission-id" type="text" />
          </div>
          <div>
            <label for="scenario">Scenario</label>
            <sl-select id="scenario" name="scenario">
              ${this._scenarios.map(
                (scenario) => html`
                  <sl-option value="${scenario.replaceAll(" ", "-")}"
                    >${scenario}
                  </sl-option>
                `
              )}
            </sl-select>
          </div>
          <sl-button type="submit" variant="primary"> Run Scenario</sl-button>
        </form>
      </div>
    `;
  }
}

export default SimulationConnectorButtonCe;

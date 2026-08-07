// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { css, html } from "https://esm.sh/lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/select/select.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";

class SimulationConnectorButtonCe extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    baseUrl: { attribute: "base-url" },
    _scenarios: { type: Array },
    _permissionId: { type: String },
    _startDate: { type: String },
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
    this._permissionId = crypto.randomUUID();
    this._startDate = new Date().toISOString().split("T")[0];
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

  handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    this.submitScenario(formData).catch((error) =>
      this.error(error.message ?? error)
    );
  }

  async submitScenario(formData) {
    let creationDateTime = formData.get("start-date");
    if (creationDateTime) {
      creationDateTime += "T00:00:00.000Z";
    } else {
      creationDateTime = new Date().toISOString();
    }
    const metadata = {
      connectionId: this.connectionId,
      permissionId: formData.get("permission-id"),
      dataNeedId: this.dataNeedId,
      creationDateTime,
    };
    const file = formData.get("scenario-file");
    const scenario = formData.get("scenario");

    let response;
    if (file?.name) {
      response = await this.postFileScenario(file, metadata);
    } else if (scenario) {
      response = await this.postSelectedScenario(scenario, metadata);
    } else {
      throw new Error("Please select a scenario or upload a scenario file");
    }

    const data = await response.json().catch(() => null);
    if (!response.ok) {
      const error = data.errors?.map((error) => error.message).join("\n");
      throw new Error(
        error ?? "Something went wrong when running the scenario"
      );
    }

    this.notify({
      title: "Executing Scenario!",
      message: "Your scenario is currently being executed.",
      variant: "success",
      duration: 10000,
    });
  }

  async postFileScenario(file, metadata) {
    const fileScenarioUrl = `${this.baseUrl}/scenarios/run`;
    let scenario;
    try {
      scenario = JSON.parse(await file.text());
    } catch {
      throw new Error("The uploaded scenario is not valid JSON");
    }

    return await this.postScenario(fileScenarioUrl, { metadata, scenario });
  }

  postSelectedScenario(scenario, metadata) {
    const selectedScenarioUrl = `${this.baseUrl}/scenarios/${scenario.replaceAll("-", " ")}/run`;
    return this.postScenario(selectedScenarioUrl, metadata);
  }

  postScenario(url, body) {
    return fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });
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
            <sl-input
              id="start-date"
              name="start-date"
              type="date"
              label="Start Date (in UTC)"
              value="${this._startDate}"
              valueAsDate="${this._startDate}"
              filled
            />
          </div>
          <br />
          <div>
            <sl-select id="scenario" name="scenario" label="Scenario">
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
          <div>
            <label for="scenario-file">Or upload a scenario:</label>
            <input
              id="scenario-file"
              name="scenario-file"
              type="file"
              accept=".json,application/json"
            />
          </div>
          <br />
          <sl-button type="submit" variant="primary">Run Scenario</sl-button>
        </form>
      </div>
    `;
  }
}

export default SimulationConnectorButtonCe;

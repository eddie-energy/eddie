import { css, html, LitElement } from "https://esm.sh/lit";

class SimulationConnectorButtonCe extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
  };
  /**
   * CSS variables for Bootstrap. Unfortunately, Bootstrap defines its variables
   * on :root only and not on :host. So they need to be redefined to make them
   * usable inside a custom element.
   *
   * @type {CSS}
   */
  static styles = css`
    :host {
      --bs-border-width: 1px;
      --bs-border-color: #dee2e6;
    }
  `;

  constructor() {
    super();
  }

  launchSimulation() {
    const url = new URL(import.meta.url);
    url.pathname = url.pathname.replace(/\/[^/]*$/, "/simulation.html");
    url.hash =
      "?connectionId=" +
      encodeURIComponent(this.connectionId) +
      "&dataNeedId=" +
      encodeURIComponent(this.dataNeedId);
    window.open(url, "_blank");
  }

  render() {
    return html`
      <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha3/dist/css/bootstrap.min.css"
      />
      <div class="container">
        <h4>Simulation MDA</h4>
        <p>
          The following information about the data need of the EP application
          was given:
        </p>
        <table class="table table-striped table-bordered">
          <thead>
            <tr>
              <th>Key</th>
              <th>Value</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>connectionId (ce parameter)</td>
              <td>${this.connectionId}</td>
            </tr>
            <tr>
              <td>id</td>
              <td>${this.dataNeedId}</td>
            </tr>
          </tbody>
        </table>
        <button
          @click="${this.launchSimulation}"
          type="button"
          class="btn btn-primary"
        >
          Launch simulation
        </button>
      </div>
    `;
  }
}

export default SimulationConnectorButtonCe;

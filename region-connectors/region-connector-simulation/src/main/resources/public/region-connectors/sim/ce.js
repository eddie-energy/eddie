import { css, html, LitElement } from "https://esm.sh/lit";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";

const baseUrl = import.meta.url.substring(0, import.meta.url.lastIndexOf("/"));

class SimulationConnectorButtonCe extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
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
      </div>
    `;
  }
}

export default SimulationConnectorButtonCe;

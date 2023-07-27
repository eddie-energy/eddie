import {css, html, LitElement} from "https://esm.sh/lit";

class SimulationConnectorButtonCe extends LitElement {

    static properties = {connectionid: {}};
    static styles = css``;

    constructor() {
        super();
    }

    launchSimulation() {
        const url = new URL(import.meta.url);
        url.pathname = url.pathname.replace(/\/[^/]*$/, "/simulation.html");
        url.hash = "?connectionid=" + this.connectionid;
        window.open(url, "_blank");
    }

    render() {
        return html`
            <link rel="stylesheet"
                  href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha3/dist/css/bootstrap.min.css">
            <div class="container">
                <h4>Simulation MDA</h4>
                <p> Please open window for starting the simulation for connection id
                    <span class="font-monospace">${this.connectionid}</span>.</p>
                <button @click=${this.launchSimulation} type="button" class="btn btn-primary">
                    Launch simulation
                </button>
            </div>
        `;
    }
}

export default SimulationConnectorButtonCe;

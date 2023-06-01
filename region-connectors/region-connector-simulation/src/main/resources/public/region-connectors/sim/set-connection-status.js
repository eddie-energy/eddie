import {html, css, LitElement} from "https://esm.sh/lit";

class SetConnectionStatusCe extends LitElement {

    static properties = {connectionid: {}, _statusValues: {}};
    static styles = css``;

    constructor() {
        super();
        this._statusValues = [];
        fetch("api/consent-status-values")
            .then( res => res.json())
            .then( json => this._statusValues = json);
    }

    switchStatus(e) {
        const consentStatus = e.target.value;
        console.log(`switching connection status of ${this.connectionid} to ${consentStatus}`);
        const body = JSON.stringify({connectionId: this.connectionid, consentStatus});
        fetch("api/consent-status", {method: "POST", body});
    }

    render() {
        return html`
            <link rel="stylesheet"
                  href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha3/dist/css/bootstrap.min.css">
            <div class="card">
                <div class="card-header">
                    Connection Status
                </div>
                <div class="card-body">
                    <div class="row align-items-start">
                        ${this._statusValues.map(val => html`
                            <div class="col form-check">
                                <input class="form-check-input" type="radio"
                                       name="connectionStatus" id="connectionStatus${val}" value="${val}"
                                       @change=${this.switchStatus}>
                                <label class="form-check-label" for="connectionStatus${val}">${val}</label>
                            </div>
                        `)}
                    </div>
                </div>
            </div>
        `;
    }
}

window.customElements.define("set-connection-status", SetConnectionStatusCe);
export default SetConnectionStatusCe;

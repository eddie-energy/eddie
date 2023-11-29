import { html, LitElement } from "lit";

const BASE_URL = new URL(import.meta.url).href.replace("ce.js", "");
const REQUEST_URL = BASE_URL + "permission-request";

class PermissionRequestForm extends LitElement {
  render() {
    return html`
      <div>
        <h1>THIS IS AIIDA!!!</h1>
      </div>
    `;
  }
}

export default PermissionRequestForm;

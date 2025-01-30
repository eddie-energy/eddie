import { html } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    jumpOffUrl: { attribute: "jump-off-url" },
    _requestStatus: { type: String },
    _shortUrlIdentifier: { type: String },
  };

  constructor() {
    super();
  }

  handleSubmit(event) {
    event.preventDefault();

    const payload = {
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId,
    };

    this.createPermissionRequest(payload).catch((error) => this.error(error));
  }

  render() {
    return html`
      <div>
        <form id="request-form">
          <sl-button type="submit" variant="primary">Create</sl-button>
        </form>
      </div>
    `;
  }
}

export default PermissionRequestForm;

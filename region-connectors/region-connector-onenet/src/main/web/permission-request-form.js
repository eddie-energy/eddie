// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { html } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    accountingPointId: { attribute: "accounting-point-id" },
  };

  constructor() {
    super();
  }

  handleSubmit(event) {
    event.preventDefault();

    // This will be the PermissionRequestForCreation in the region connector
    const jsonData = {};
    // TODO: Retrieve data from form fields and put into jsonData
    jsonData.connectionId = this.connectionId;
    jsonData.dataNeedId = this.dataNeedId;

    this.createPermissionRequest(jsonData)
      .then((response) => {
        // TODO: Do something with the response, or ignore
      })
      .catch((error) => {
        // TODO: Do something with the error if needed
        this.error(error);
      });
  }

  render() {
    return html`
      <div>
        <form id="request-form">
          <sl-button type="submit" variant="primary"> Create </sl-button>
        </form>
        <h1>OneNet Region Connector Element</h1>
        <h2>TBD</h2>
        <!-- TODO: Create form to request required data from final customer, such as metering point ID etc-->
      </div>
    `;
  }
}

export default PermissionRequestForm;

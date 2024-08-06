import EddieConnectButton from "./eddie-connect-button.js";
import EddieNotificationHandler from "./eddie-notification-handler.js";
import EddieRequestStatusHandler from "./eddie-request-status-handler.js";

import "./data-need-summary.js";

customElements.define("eddie-connect-button", EddieConnectButton);
customElements.define("eddie-notification-handler", EddieNotificationHandler);
customElements.define(
  "eddie-request-status-handler",
  EddieRequestStatusHandler
);

// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import EddieConnectButton from "./eddie-connect-button.js";
import EddieNotificationHandler from "./eddie-notification-handler.js";
import EddieRequestStatusHandler from "./eddie-request-status-handler.js";

import "./data-need-summary.js";
import "./step-indicator.js";

customElements.define("eddie-connect-button", EddieConnectButton);
customElements.define("eddie-notification-handler", EddieNotificationHandler);
customElements.define(
  "eddie-request-status-handler",
  EddieRequestStatusHandler
);

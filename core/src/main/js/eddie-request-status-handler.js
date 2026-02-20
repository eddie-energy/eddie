// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { html, LitElement } from "lit";

import STATUS_MESSAGES from "./permission-process-status-messages.json";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";

const VARIANT_ICONS = {
  info: "info-circle",
  success: "check2-circle",
  warning: "exclamation-triangle",
  danger: "exclamation-octagon",
};

const STATUS_VIEWS = {
  UNABLE_TO_SEND: "unable-to-send",
  ACCEPTED: "accepted",
  REJECTED: "rejected",
  TIMED_OUT: "timed-out",
  INVALID: "invalid",
  UNFULFILLABLE: "unfulfillable",
  FULFILLED: "fulfilled",
};

class EddieRequestStatusHandler extends LitElement {
  static properties = {
    permissionStates: { type: Object },
  };

  constructor() {
    super();

    /**
     * Maps permissions to their current status and optional message.
     * @type {Record<string, { status: string, message?: string }>}
     */
    this.permissionStates = {};

    this.addEventListener(
      "eddie-request-status",
      ({ detail: { message, permissionId, status } }) => {
        this.permissionStates[permissionId] = {
          status: status,
          message: message,
        };

        // Lit does not detect mutation
        this.requestUpdate();

        for (const viewStatus of Object.keys(STATUS_VIEWS)) {
          if (
            Object.values(this.permissionStates).every(
              ({ status }) => status === viewStatus
            )
          ) {
            // Request view change
            this.dispatchEvent(
              new Event(`eddie-view-${STATUS_VIEWS[viewStatus]}`, {
                composed: true,
                bubbles: true,
              })
            );
            // Skip other status checks as we already know which view to show
            break;
          }
        }
      }
    );
  }

  render() {
    return html`
      <slot></slot>
      ${Object.entries(this.permissionStates).map(
        ([_, { message: detailsMessage, status }]) => {
          const { title, message, variant } = STATUS_MESSAGES[status];
          return html`
            <br />
            <sl-alert variant="${variant}" open>
              <sl-icon name="${VARIANT_ICONS[variant]}" slot="icon"></sl-icon>
              <p>
                <strong>Status: ${title}</strong><br />
                ${message} ${detailsMessage ?? ""}
              </p>
            </sl-alert>
          `;
        }
      )}
    `;
  }
}

export default EddieRequestStatusHandler;

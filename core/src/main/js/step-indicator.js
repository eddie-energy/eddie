import { css, html, LitElement } from "lit";
import { classMap } from "lit/directives/class-map.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.19.0/cdn/components/tooltip/tooltip.js";

class StepIndicator extends LitElement {
  static properties = {
    step: { attribute: "step", type: Number },
    error: { attribute: "error", type: Object },
  };

  static styles = css`
    ol {
      display: flex;
      justify-content: space-between;
      align-items: center;
      list-style: none;
      margin: 0 auto;
      padding: 0;
      background: linear-gradient(
        180deg,
        transparent calc(50% - 1px),
        var(--sl-color-neutral-300) calc(50%),
        transparent calc(50% + 1px)
      );
    }

    span {
      border: 1px solid;
      border-radius: 100%;
      font-size: 14px;
      height: 24px;
      width: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      user-select: none;
    }

    span {
      background: var(--sl-color-success-600);
      border-color: var(--sl-color-success-600);
      color: white;
    }

    li.current:not(:last-child) span {
      background: var(--sl-color-primary-600);
      border-color: var(--sl-color-primary-600);
    }

    li.current ~ li span {
      background: white;
      border-color: var(--sl-color-neutral-300);
      color: black;
    }

    li.current.error span {
      background: var(--sl-color-danger-600);
      border-color: var(--sl-color-danger-600);
    }
  `;

  render() {
    const steps = [
      "Confirm the usage policy for the requested data.",
      "Select your country and permission administrator.",
      "Accept the permission request.",
      "Provide required information and send the permission request.",
      "Confirm the result and close the dialog.",
    ];

    return html`<ol>
      ${steps.map(
        (step, index) => html`
          <li
            class="${classMap({
              current: index === this.step - 1,
              error: index === this.step - 1 && !!this.error,
            })}"
          >
            <sl-tooltip content="${step}">
              <span>${index + 1}</span>
            </sl-tooltip>
          </li>
        `
      )}
    </ol>`;
  }
}

customElements.define("eddie-step-indicator", StepIndicator);

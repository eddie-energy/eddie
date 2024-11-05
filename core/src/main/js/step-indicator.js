import { css, html, LitElement } from "lit";
import { classMap } from "lit/directives/class-map.js";

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

    li {
      border: 1px var(--sl-color-neutral-300) solid;
      border-radius: 100%;
      height: 24px;
      width: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    ol {
      counter-reset: step;
    }

    li::before {
      counter-increment: step;
      content: counter(step);
      font-size: 14px;
    }

    li {
      background: var(--sl-color-success-600);
      border-color: var(--sl-color-success-600);
      color: white;
    }

    li.current:not(:last-child) {
      background: var(--sl-color-primary-600);
      border-color: var(--sl-color-primary-600);
    }

    li.current ~ li {
      background: white;
      border-color: var(--sl-color-neutral-300);
      color: black;
    }

    li.current.error {
      background: var(--sl-color-danger-600);
      border-color: var(--sl-color-danger-600);
    }

    span {
      /* only be visible to screen readers */
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border-width: 0;
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
            <span>${step}</span>
          </li>
        `
      )}
    </ol>`;
  }
}

customElements.define("eddie-step-indicator", StepIndicator);

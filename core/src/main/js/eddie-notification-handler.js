const VARIANT_ICONS = {
  info: "info-circle",
  success: "check2-circle",
  warning: "exclamation-triangle",
  danger: "exclamation-octagon",
};

class EddieNotificationHandler extends HTMLElement {
  connectedCallback() {
    this.addEventListener("eddie-notification", this.handleNotification);
  }

  disconnectedCallback() {
    this.removeEventListener("eddie-notification", this.handleNotification);
  }

  handleNotification(event) {
    this.renderNotification(event.detail);
  }

  renderNotification({
    title,
    message,
    reason = "",
    variant = "info",
    duration = Infinity,
    extraFunctionality = [],
  }) {
    const template = document.createElement("template");
    template.innerHTML = /* HTML */ `
      <sl-alert
        variant="${variant}"
        duration="${duration}"
        closable
        open
        style="margin-top: var(--sl-spacing-medium)"
      >
        <sl-icon name="${VARIANT_ICONS[variant]}" slot="icon"></sl-icon>
        <p>
          <strong>${title}</strong><br />
          ${message}${reason && " Reason: " + reason}
        </p>
      </sl-alert>
    `;

    extraFunctionality.forEach((element) =>
      template.content.firstElementChild.append(element)
    );
    this.appendChild(template.content);
  }
}

export default EddieNotificationHandler;

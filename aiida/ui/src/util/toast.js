const variant = {
  info: 'primary',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
}

const icon = {
  info: 'info-circle',
  success: 'check2-circle',
  warning: 'exclamation-triangle',
  danger: 'exclamation-octagon',
}

/**
 * @param {string} message
 * @param {"info" | "success" | "warning" | "danger"} severity
 * @param {number} duration
 */
export function notify(message, severity = 'info', duration = 10000) {
  const alert = Object.assign(document.createElement('sl-alert'), {
    variant: variant[severity],
    closable: true,
    duration: duration,
    innerHTML: `
        <sl-icon name="${icon[severity]}" slot="icon"></sl-icon>
        ${message}
      `,
  })

  document.body.append(alert)
  customElements.whenDefined('sl-alert').then(() => alert.toast())
}

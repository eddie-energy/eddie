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

export function notify(
  message: string,
  severity: 'info' | 'success' | 'warning' | 'danger' = 'info',
  duration: number = 10000,
) {
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
  //@ts-expect-error
  // temporary because we will delete it later
  customElements.whenDefined('sl-alert').then(() => alert.toast())
}

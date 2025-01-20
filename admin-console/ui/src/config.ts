const { VITE_EDDIE_PUBLIC_URL, VITE_EDDIE_ADMIN_CONSOLE_URL } = import.meta.env

const EDDIE_PUBLIC_URL =
  THYMELEAF_EDDIE_PUBLIC_URL ?? VITE_EDDIE_PUBLIC_URL ?? 'http://localhost:8080'
const EDDIE_ADMIN_CONSOLE_URL =
  THYMELEAF_EDDIE_ADMIN_CONSOLE_URL ??
  VITE_EDDIE_ADMIN_CONSOLE_URL ??
  'http://localhost:9090/outbound-connectors/admin-console'

export const CSRF_HEADER = THYMELEAF_CSRF_HEADER
export const CSRF_TOKEN = THYMELEAF_CSRF_TOKEN

export const PERMISSIONS_API_URL = `${EDDIE_ADMIN_CONSOLE_URL}/statusMessages`
export const TERMINATION_API_URL = `${EDDIE_ADMIN_CONSOLE_URL}/terminate`
export const DATA_NEEDS_API_URL = `${EDDIE_PUBLIC_URL}/data-needs/api`
export const REGION_CONNECTOR_API_URL = `${EDDIE_PUBLIC_URL}/api/region-connectors-metadata`
export const REGION_CONNECTOR_HEALTH_API_URL = `${EDDIE_PUBLIC_URL}/actuator/health`

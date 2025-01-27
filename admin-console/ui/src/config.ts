const { VITE_EDDIE_PUBLIC_URL, VITE_EDDIE_ADMIN_CONSOLE_URL, VITE_EDDIE_MANAGEMENT_URL } =
  import.meta.env

const CORE_URL = THYMELEAF_EDDIE_PUBLIC_URL ?? VITE_EDDIE_PUBLIC_URL ?? 'http://localhost:8080'
const ADMIN_URL =
  THYMELEAF_EDDIE_ADMIN_CONSOLE_URL ??
  VITE_EDDIE_ADMIN_CONSOLE_URL ??
  'http://localhost:9090/outbound-connectors/admin-console'
const MANAGEMENT_URL =
  THYMELEAF_EDDIE_MANAGEMENT_URL ?? VITE_EDDIE_MANAGEMENT_URL ?? 'http://localhost:9090/management'

export const CSRF_HEADER = THYMELEAF_CSRF_HEADER
export const CSRF_TOKEN = THYMELEAF_CSRF_TOKEN

export const PERMISSIONS_API_URL = `${ADMIN_URL}/statusMessages`
export const TERMINATION_API_URL = `${ADMIN_URL}/terminate`
export const DATA_NEEDS_API_URL = `${CORE_URL}/data-needs/api`
export const REGION_CONNECTOR_API_URL = `${CORE_URL}/api/region-connectors-metadata`
export const REGION_CONNECTOR_HEALTH_API_URL = `${CORE_URL}/actuator/health`
export const REGION_CONNECTORS_SUPPORTED_FEATURES_API_URL = `${MANAGEMENT_URL}/region-connectors/supported-features`
export const REGION_CONNECTORS_SUPPORTED_DATA_NEEDS_API_URL = `${MANAGEMENT_URL}/region-connectors/supported-data-needs`

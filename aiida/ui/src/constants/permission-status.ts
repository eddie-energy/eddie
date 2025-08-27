const permissionSatus: {
  [key: string]: {
    title: string
    description: string
    isActive?: boolean
    isRevocable?: boolean
    isError?: boolean
    isOpen?: boolean
  }
} = {
  ACCEPTED: {
    title: 'Accepted',
    description: 'You accepted the permission request and it is being processed.',
    isActive: true,
    isRevocable: true,
  },
  WAITING_FOR_START: {
    title: 'Waiting for Start',
    description:
      'You accepted the permission request and it is scheduled to start at the specified start time.',
    isActive: true,
    isRevocable: true,
  },
  STREAMING_DATA: {
    title: 'Streaming Data',
    description:
      'You accepted the permission request and it is now actively streaming data to the eligible party.',
    isActive: true,
    isRevocable: true,
  },
  REJECTED: {
    title: 'Rejected',
    description: 'You rejected the permission request.',
  },
  REVOCATION_RECEIVED: {
    title: 'Revocation Received',
    description: 'You requested revocation of the permission.',
    isActive: true,
  },
  REVOKED: {
    title: 'Revoked',
    description: 'You revoked the permission.',
  },
  TERMINATED: {
    title: 'Terminated',
    description: 'The permission was terminated by the eligible party.',
  },
  FULFILLED: {
    title: 'Fulfilled',
    description: 'The expiration time of the permission was reached.',
  },
  FAILED_TO_START: {
    title: 'Failed to Start',
    description: 'An error occurred and the permission could not be started.',
    isError: true,
  },
  CREATED: {
    title: 'Created',
    description:
      'The permission has been created, but the details have not yet been fetched from the EDDIE framework.',
    isOpen: true,
  },
  FETCHED_DETAILS: {
    title: 'Fetched details',
    description: 'This permission waits for you to accept or reject it.',
    isOpen: true,
  },
  UNFULFILLABLE: {
    title: 'Unable to fulfill',
    description:
      'Your AIIDA instance is unable to fulfill the permission request, e.g. because the requested data is not available on your AIIDA instance.',
    isError: true,
  },
  FETCHED_MQTT_CREDENTIALS: {
    title: 'Fetched MQTT details',
    description:
      'This is an internal state only, the permission should be transitioned into another state automatically.',
    isActive: true,
  },
}
export default permissionSatus;
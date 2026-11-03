// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

const permissionStatus: {
  [key: string]: {
    title: string
    description: string
    isFetching?: boolean
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
  REJECTED: {
    title: 'Rejected',
    description: 'You rejected the permission request.',
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
  CREATED: {
    title: 'Created',
    description:
      'The permission has been created, but the details have not yet been fetched from the EDDIE framework.',
    isFetching: true,
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
export default permissionStatus

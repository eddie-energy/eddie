// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

const permissionStatus: {
  [key: string]: {
    title: string
    description: string
    isActive?: boolean
    isRevocable?: boolean
    isError?: boolean
  }
} = {
  ACCEPTED: {
    title: 'Accepted',
    description: 'You accepted the permission request and it is being processed.',
    isActive: true,
    isRevocable: true,
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
}
export default permissionStatus

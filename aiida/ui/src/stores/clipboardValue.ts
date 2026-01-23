// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { ref } from 'vue'

export const clipboardValue = ref('')

export const checkClipboard = async () => {
  if (clipboardValue.value !== (await navigator.clipboard.readText())) {
    clipboardValue.value = ''
  }
}

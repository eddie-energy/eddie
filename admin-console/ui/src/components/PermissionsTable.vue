<script lang="ts" setup>
import { getPermissions, getStatusMessages, terminatePermission } from '@/api'
import DataTable from 'datatables.net-vue3'
import DataTablesCore, { type Api, type ConfigColumns } from 'datatables.net-dt'
import { onMounted, ref } from 'vue'

const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })
// TODO: Pass ability to terminate in DTO
const NON_TERMINABLE_STATUSES = [
  'Cancelled',
  'Deactivated',
  'No longer available',
  'Withdrawn',
  'Deactivation',
  'Close',
  'Stop',
  'Not satisfied',
  'Rejected',
  'MALFORMED',
  'UNABLE_TO_SEND',
  'TIMED_OUT',
  'REVOKED',
  'TERMINATED',
  'UNFULFILLABLE',
  'PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT'
]

const permissions = await getPermissions()

DataTable.use(DataTablesCore)

const columns: ConfigColumns[] = [
  {
    render: '#toggle',
    orderable: false
  },
  {
    data: 'country',
    title: 'Country',
    render: (data) => {
      try {
        return COUNTRY_NAMES.of(data)
      } catch (error) {
        return data
      }
    }
  },
  { data: 'regionConnectorId', title: 'Region Connector' },
  { data: 'dataNeedId', title: 'Data Need' },
  { data: 'permissionId', title: 'Permission' },
  {
    data: 'startDate',
    title: 'Created',
    render: (data) =>
      new Intl.DateTimeFormat('en-GB', {
        dateStyle: 'short',
        timeStyle: 'medium'
      }).format(new Date(data))
  },
  { data: 'status', title: 'Status' },
  {
    title: 'Actions',
    render: '#terminate',
    orderable: false
  }
]

let dt: Api
const table = ref()

onMounted(() => {
  dt = table.value.dt
})

async function handleRowExpand(event: Event) {
  const tr = (event.target as HTMLElement).closest('tr') as Node
  const row = dt.row(tr)

  if (row.child.isShown()) {
    row.child.hide()
  } else {
    const child = await renderRowChild(row.data().permissionId)
    row.child(child).show()
  }
}

async function renderRowChild(permissionId: string) {
  const statusMessages = await getStatusMessages(permissionId)

  let html = `<ul>`
  statusMessages.forEach(({ startDate, status }) => {
    html += `
      <li>
        <span>${status}</span>
        <span>${startDate}</span>
      </li>
    `
  })
  html += `</ul>`

  return html
}

function confirmTermination(permissionId: string) {
  if (window.confirm(`Are you sure you want to terminate the permission ${permissionId}?`)) {
    terminatePermission(permissionId).catch((error) => {
      console.error(error)
      alert('Failed to terminate permission.')
    })
  }
}
</script>

<template>
  <DataTable ref="table" :columns="columns" :data="permissions" class="display dark">
    <template #toggle>
      <button class="dt-control" @click="handleRowExpand">
        <svg
          class="w-4 h-4 stroke-current"
          fill="currentColor"
          height="16"
          viewBox="0 0 24 24"
          width="16"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"></path>
        </svg>
      </button>
    </template>

    <template #terminate="{ rowData }">
      <button
        v-if="!NON_TERMINABLE_STATUSES.includes(rowData.status)"
        @click="confirmTermination(rowData.permissionId)"
      >
        Terminate
      </button>
    </template>
  </DataTable>
</template>

<script lang="ts" setup>
import {
  getLatestPermissions,
  getStatusMessages,
  type StatusMessage,
  terminatePermission
} from '@/api'
import {
  Button,
  Column,
  DataTable,
  type DataTableFilterMeta,
  type DataTableRowExpandEvent,
  IconField,
  InputIcon,
  InputText,
  Tag,
  useConfirm,
  useToast
} from 'primevue'
import { ref } from 'vue'

import { countryFlag, formatCountry } from '@/util/countries'

const filters = ref<DataTableFilterMeta>({ global: { value: null, matchMode: 'contains' } })
const expandedRows = ref({})
const rowExpansions = ref<{ [key: string]: StatusMessage[] }>({})

const permissions = ref([])
const totalRecords = ref(0)
const rowOptions = [3, 50, 100, 250, 500]
const rows = ref(rowOptions[0])

async function fetchPermissions() {
  try {
    const response = await getLatestPermissions()
    permissions.value = response.permissions
    totalRecords.value = response.totalElements
  } catch (error) {
    console.error("Error fetching permissions", error)
  }
}

/*
function onPageChange(event: {first: number, rows: number}) {
  fetchPermissions(event.first / event.rows, event.rows)
}
*/

function loadPermissions() {
  //TODO
  console.log("loading..")
}

function formatDate(date: string) {
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'short',
    timeStyle: 'medium'
  }).format(new Date(date))
}

function getStatusSeverity(status: string) {
  switch (status) {
    case 'ACCEPTED':
      return 'success'
    case 'MALFORMED':
    case 'UNABLE_TO_SEND':
    case 'INVALID':
    case 'UNFULFILLABLE':
      return 'danger'
    default:
      return 'secondary'
  }
}

async function onRowExpand(event: DataTableRowExpandEvent) {
  const id = event.data.permissionId
  rowExpansions.value[id] ||= await getStatusMessages(id)
}

const confirm = useConfirm()
const toast = useToast()

function confirmTermination(permissionId: string) {
  confirm.require({
    message: 'Are you sure you want to terminate this permission?',
    header: 'Danger Zone',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {
      label: 'Cancel',
      severity: 'secondary',
      outlined: true
    },
    acceptProps: {
      label: 'Delete',
      severity: 'danger'
    },
    accept: () => {
      terminatePermission(permissionId)
        .then(() => {
          toast.add({
            severity: 'success',
            summary: 'Permission terminated',
            detail: `Permission with ID ${permissionId} has been terminated.`,
            life: 3000
          })
        })
        .catch(() => {
          toast.add({
            severity: 'error',
            summary: 'Failed to terminate permission',
            detail: `Failed to terminate permission with ID ${permissionId}.`,
            life: 3000
          })
        })
    }
  })
}

fetchPermissions()
</script>

<template>
  <DataTable
    :value="permissions"
    data-key="permissionId"
    v-model:expanded-rows="expandedRows"
    @row-expand="onRowExpand"
    paginator
    :rows
    :rows-per-page-options="rowOptions"
    v-model:filters="filters"
    :global-filter-fields="[
      'country',
      'regionConnectorId',
      'dataNeedId',
      'permissionId',
      'startDate',
      'status',
      'cimStatus'
    ]"
    removable-sort
    scrollable
  >
    <template #header>
      <IconField>
        <InputIcon>
          <i class="pi pi-search" />
        </InputIcon>
        <InputText v-model="filters.global.value" placeholder="Keyword Search" />
      </IconField>
    </template>

    <template #paginatorstart>
      <p>Loaded <b>{{ permissions?.length }}</b> out of {{ totalRecords }} permissions. <a @click="loadPermissions()">Load more...</a></p>
    </template>

    <Column expander />
    <Column field="country" header="Country" sortable>
      <template #body="slotProps">
        <div>
          {{ countryFlag(slotProps.data.country) }}
          {{ formatCountry(slotProps.data.country) }}
        </div>
      </template>
    </Column>
    <Column field="regionConnectorId" header="Region Connector" sortable />
    <Column field="dataNeedId" header="Data Need" sortable />
    <Column field="permissionId" header="Permission" sortable />
    <Column field="startDate" header="Last updated" sortable>
      <template #body="slotProps">
        {{ formatDate(slotProps.data.startDate) }}
      </template>
    </Column>
    <Column field="status" header="Status" sortable>
      <template #body="slotProps">
        <Tag :severity="getStatusSeverity(slotProps.data.status)">
          <span class="status-tag-value">{{ slotProps.data.status }}</span>
        </Tag>
      </template>
    </Column>
    <Column field="cimStatus" header="CIM Status" sortable />
    <Column header="Actions">
      <template #body="slotProps">
        <Button
          v-if="slotProps.data.status === 'ACCEPTED'"
          label="Terminate"
          severity="danger"
          size="small"
          outlined
          @click="confirmTermination(slotProps.data.permissionId)"
        />
      </template>
    </Column>

    <template #expansion="slotProps">
      <ul>
        <template v-for="row in rowExpansions[slotProps.data.permissionId]">
          <li>
            <Tag :value="row.status" :severity="getStatusSeverity(row.status)" />
            <span>{{ formatDate(row.startDate) }}</span>
          </li>
        </template>
      </ul>
    </template>
  </DataTable>
</template>

<style scoped>
ul {
  padding: 0;
  display: grid;
  gap: 0.5rem;
}

li {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

/* Prevent search from overflowing on mobile */
input {
  max-width: 100%;
}

/* Prevent long status texts from overflowing table columns */
.status-tag-value {
  text-overflow: ellipsis;
  max-width: 10ch;
  overflow: hidden;
}

a:hover {
  cursor:pointer;
}
</style>

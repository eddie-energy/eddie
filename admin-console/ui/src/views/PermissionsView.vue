<!-- SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script lang="ts" setup>
import {
  getPermissionsPaginated,
  getStatusMessages,
  type StatusMessage,
  terminatePermission
} from '@/api'
import {
  Button,
  Column,
  DataTable,
  type DataTableExpandedRows,
  type DataTableRowExpandEvent,
  IconField,
  InputIcon,
  InputText,
  useConfirm,
  useToast
} from 'primevue'
import { onMounted, ref } from 'vue'

import { countryFlag, formatCountry } from '@/util/countries'
import PermissionStatusCard from '@/components/PermissionStatusCard.vue'
import StatusTag from '@/components/StatusTag.vue'

const confirm = useConfirm()
const toast = useToast()

const permissions = ref<StatusMessage[]>([])
const totalRecords = ref(0)
const loading = ref(true)

const filters = ref({ global: { value: null, matchMode: 'contains' } })
const selectedRows = ref<StatusMessage[]>([])
const expandedRows = ref<DataTableExpandedRows>({})
const rowExpansions = ref<{ [key: string]: StatusMessage[] }>({})

let loadedPage = 0

async function fetchPermissions(page: number = 0, size: number = 500) {
  try {
    const response = await getPermissionsPaginated(page, size)
    permissions.value.push(...response.content)
    totalRecords.value = response.page.totalElements
    loadedPage++
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Failed to fetch permissions',
      detail: `The request to fetch additional permissions has failed`,
      life: 3000
    })
  }
}

function loadMorePermissions() {
  fetchPermissions(loadedPage)
}

function formatDate(date: string) {
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'short',
    timeStyle: 'medium'
  }).format(new Date(date))
}

async function onRowExpand(event: DataTableRowExpandEvent) {
  const id = event.data.permissionId
  rowExpansions.value[id] ||= (await getStatusMessages(id)).slice().reverse()
}

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

function retransmitSelected() {
  // TODO: GH-1713
  console.debug(selectedRows.value)
  alert('Not implemented. See GH-1713.')
}

function terminateSelected() {
  for (const row of selectedRows.value) {
    confirmTermination(row.permissionId)
  }
}

onMounted(async () => {
  await fetchPermissions()
  loading.value = false
})
</script>

<template>
  <h1>Permissions</h1>
  <p>The central hub for permission monitoring and management.</p>

  <DataTable
    data-key="permissionId"
    :value="permissions"
    :rows="50"
    :rows-per-page-options="[50, 100, 250, 500]"
    paginator
    removable-sort
    scrollable
    @row-expand="onRowExpand"
    v-model:selection="selectedRows"
    v-model:expanded-rows="expandedRows"
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
  >
    <template #header>
      <div class="header-actions">
        <IconField>
          <InputIcon>
            <i class="pi pi-search" />
          </InputIcon>
          <InputText v-model="filters.global.value" placeholder="Keyword Search" />
        </IconField>

        <Button label="Retransmit selected" rounded @click="retransmitSelected"></Button>

        <Button
          label="Terminate selected"
          severity="danger"
          rounded
          @click="terminateSelected"
        ></Button>
      </div>
    </template>

    <template #empty>No permissions found.</template>

    <template #loading>Loading permissions. Please wait.</template>

    <template #paginatorstart>
      <p>
        Loaded <b>{{ permissions?.length }}</b> out of {{ totalRecords }} permissions.
        <a v-if="permissions.length < totalRecords" @click="loadMorePermissions()">Load more...</a>
      </p>
    </template>

    <Column expander />
    <Column selectionMode="multiple"></Column>
    <Column field="country" header="Country">
      <template #body="slotProps">
        <span class="country">
          {{ countryFlag(slotProps.data.country) }}
          {{ formatCountry(slotProps.data.country) }}
        </span>
      </template>
    </Column>
    <Column field="regionConnectorId" header="Region Connector" />
    <Column field="dataNeedId" header="Data Need" />
    <Column field="permissionId" header="Permission" />
    <Column field="startDate" header="Last updated" sortable>
      <template #body="slotProps">
        {{ formatDate(slotProps.data.startDate) }}
      </template>
    </Column>
    <Column field="status" header="Status">
      <template #body="slotProps">
        <StatusTag :status="slotProps.data.status" />
      </template>
    </Column>
    <Column field="cimStatus" header="CIM Status" />
    <Column header="Actions">
      <template #body="slotProps">
        <Button
          v-if="slotProps.data.status === 'ACCEPTED'"
          label="Terminate"
          severity="danger"
          rounded
          @click="confirmTermination(slotProps.data.permissionId)"
        />
      </template>
    </Column>

    <template #expansion="slotProps">
      <ul class="permission-states">
        <li
          v-for="row in rowExpansions[slotProps.data.permissionId]"
          :key="row.status + row.startDate"
        >
          <PermissionStatusCard :status="row.status" :datetime="row.startDate" />
        </li>
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

.header-actions {
  display: grid;
  gap: 0.75rem;

  @media (width >= 80rem) {
    grid-template-columns: 1fr auto auto;
  }
}

.country {
  text-wrap: nowrap;
}

.permission-states {
  display: flex;
  flex-wrap: wrap;
  align-items: start;
  justify-content: start;
  gap: 1rem;
}

a:hover {
  cursor: pointer;
}
</style>

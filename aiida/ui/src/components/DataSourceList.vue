<script setup lang="ts">
import { deleteDataSource, toggleDataSource } from '@/api'
import DataSourceCard from '@/components/DataSourceCard.vue'
import { onMounted } from 'vue'
import { dataSources, fetchDataSources } from '@/stores/dataSources'
import type { AiidaDataSource } from '@/types'
import { useConfirmDialog } from '@/composables/confirm-dialog'

const { confirm } = useConfirmDialog()
const emit = defineEmits(['edit', 'reset'])

async function handleDelete(id: string) {
  if (
    await confirm(
      'Delete Data Source',
      'Are you sure you want to delete this data source? This action cannot be undone.',
      'Delete',
    )
  ) {
    deleteDataSource(id).then(() => fetchDataSources())
  }
}

const handleEnableToggle = (datasource: AiidaDataSource) => {
  toggleDataSource(datasource.id, datasource).then(() => fetchDataSources())
}

onMounted(() => {
  fetchDataSources()
})
</script>

<template>
  <div class="datasource-list-wrapper">
    <TransitionGroup class="layout" tag="div" name="list">
      <DataSourceCard
        v-for="dataSource in dataSources"
        :key="JSON.stringify(dataSource)"
        :data-source
        @edit="emit('edit', dataSource)"
        @delete="handleDelete(dataSource.id)"
        @reset="emit('reset', dataSource.id)"
        @enableToggle="handleEnableToggle(dataSource)"
      />
    </TransitionGroup>
  </div>
</template>

<style scoped>
.datasource-list-wrapper {
  margin-bottom: calc(var(--mobile-header-height) / 1.5);
}
.layout {
  display: grid;
  position: relative;
  overflow-x: hidden;
  gap: 1rem;
  max-height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-color: var(--eddie-primary) var(--light);
  scrollbar-gutter: stable;

  @media (min-width: 1024px) {
    grid-template-columns: 1fr 1fr;
  }
}

.list-move,
.list-enter-active,
.list-leave-active {
  transition:
    transform 0.5s ease,
    opacity 0.5s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.list-leave-active {
  position: absolute;
  display: none;
  width: 50%;
  height: 100%;
}
</style>

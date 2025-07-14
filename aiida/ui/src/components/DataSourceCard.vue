<script setup>
import { BASE_URL, deleteDataSource } from '@/api.js'

/** @type {{ dataSource: AiidaDataSource }} */
const { dataSource } = defineProps(['dataSource'])

const emit = defineEmits(['edit', 'reset'])

const { asset, dataSourceType, enabled, id, mqttSettings, name, simulationPeriod } = dataSource

function handleDelete() {
  confirm('This action will remove the given data source.') && deleteDataSource(id)
}
</script>

<template>
  <sl-details>
    <span slot="summary">
      <strong>{{ name }}</strong>
      <br />
      <small>{{ dataSourceType }}</small>
    </span>

    <dl class="details-list">
      <dt>ID:</dt>
      <dd>{{ id }}</dd>

      <dt>Asset:</dt>
      <dd>{{ asset }}</dd>

      <dt>Type:</dt>
      <dd>{{ dataSourceType }}</dd>

      <template v-if="simulationPeriod">
        <dt>Simulation Period:</dt>
        <dd>{{ simulationPeriod }} seconds</dd>
      </template>

      <template v-if="mqttSettings">
        <dt>MQTT Server URI:</dt>
        <dd>{{ mqttSettings.externalHost }}</dd>
        <dt>MQTT Topic:</dt>
        <dd>{{ mqttSettings.subscribeTopic }}</dd>
        <dt>MQTT Username:</dt>
        <dd>{{ mqttSettings.username }}</dd>
        <dt>MQTT Password:</dt>
        <dd>
          <sl-button size="small" @click="emit('reset')">Reset password</sl-button>
        </dd>
        <dt>MQTT Certificate</dt>
        <dd>
          <sl-button
            size="small"
            :href="BASE_URL + '/mqtt/download/tls-certificate'"
            download="certificate.pem"
            target="_blank"
            >Download certificate
          </sl-button>
        </dd>
      </template>

      <dt>Enabled:</dt>
      <dd>{{ enabled }}</dd>
    </dl>

    <br />
    <sl-button @click="emit('edit')">Edit</sl-button>
    <sl-button @click="handleDelete">Delete</sl-button>
  </sl-details>
</template>

<style scoped>
sl-button + sl-button {
  margin-left: 0.5rem;
}
</style>

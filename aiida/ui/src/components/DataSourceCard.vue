<script setup lang="ts">
import { BASE_URL } from '@/api'
import Button from '@/components/Button.vue'
import TrashIcon from '@/assets/icons/TrashIcon.svg'
import PenIcon from '@/assets/icons/PenIcon.svg'
import DataSourceIcon from '@/components/DataSourceIcon.vue'

const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

const { dataSource } = defineProps(['dataSource'])

const emit = defineEmits(['edit', 'delete', 'reset'])

const {
  countryCode,
  asset,
  dataSourceType,
  enabled,
  id,
  mqttSettings,
  name,
  simulationPeriod,
  icon = 'electricity',
} = dataSource
</script>

<template>
  <article class="card">
    <header class="header">
      <DataSourceIcon :icon />
      <h2 class="heading-4 headline">{{ name }}</h2>
      <span class="text-xsmall">{{ dataSourceType }}</span>
    </header>

    <dl class="fields">
      <div>
        <dt>ID</dt>
        <dd>{{ id }}</dd>
      </div>

      <template v-if="countryCode">
        <div>
          <dt>Country</dt>
          <dd>{{ COUNTRY_NAMES.of(countryCode) }}</dd>
        </div>
      </template>

      <div>
        <dt>Asset</dt>
        <dd>{{ asset }}</dd>
      </div>

      <div>
        <dt>Type</dt>
        <dd>{{ dataSourceType }}</dd>
      </div>

      <template v-if="simulationPeriod">
        <div>
          <dt>Simulation Period</dt>
          <dd>{{ simulationPeriod }} seconds</dd>
        </div>
      </template>

      <template v-if="mqttSettings">
        <div>
          <dt>MQTT Server URI</dt>
          <dd>{{ mqttSettings.externalHost }}</dd>
        </div>
        <div>
          <dt>MQTT Topic</dt>
          <dd>{{ mqttSettings.subscribeTopic }}</dd>
        </div>
        <div>
          <dt>MQTT Username</dt>
          <dd>{{ mqttSettings.username }}</dd>
        </div>
        <div class="button-field">
          <dt>MQTT Password</dt>
          <dd>
            <Button button-style="secondary" @click="emit('reset')">Reset password</Button>
          </dd>
        </div>
        <div class="button-field">
          <dt>MQTT Certificate</dt>
          <dd>
            <Button
              button-style="secondary"
              :href="BASE_URL + '/mqtt/download/tls-certificate'"
              download="certificate.pem"
              target="_blank"
            >
              Download certificate
            </Button>
          </dd>
        </div>
      </template>

      <div>
        <dt>Enabled</dt>
        <dd>{{ enabled }}</dd>
      </div>
    </dl>

    <div class="actions">
      <Button button-style="error" @click="emit('delete')"><TrashIcon />Delete</Button>
      <Button @click="emit('edit')"><PenIcon />Edit</Button>
    </div>
  </article>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--eddie-primary);
  border-radius: var(--border-radius);
  padding: var(--spacing-lg);
  background: var(--light);
}

.headline {
  word-break: break-word;
}

.fields {
  display: grid;
  gap: var(--spacing-sm);
  color: var(--eddie-grey-medium);
}

.fields > div {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: var(--spacing-md);
}

.fields dd {
  text-align: right;
}

.fields > div:not(.button-field),
.fields > .button-field > dt {
  padding: var(--spacing-xs) var(--spacing-sm);
  border: 1px solid var(--eddie-grey-light);
  border-radius: var(--border-radius);
}

.header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: var(--spacing-md);
  align-items: end;
  color: var(--eddie-primary);
  margin-bottom: var(--spacing-md);
  font-weight: 600;
}

.actions {
  flex-grow: 1;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: var(--spacing-md);
  margin-top: 0.75rem;
}
</style>

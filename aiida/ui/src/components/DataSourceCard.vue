<script setup lang="ts">
import { BASE_URL } from '@/api'
import Button from '@/components/Button.vue'
import TrashIcon from '@/assets/icons/TrashIcon.svg'
import PenIcon from '@/assets/icons/PenIcon.svg'
import ElectricityIcon from '@/assets/icons/ElectricityIcon.svg'
import HeatIcon from '@/assets/icons/HeatIcon.svg'
import MeterIcon from '@/assets/icons/MeterIcon.svg'
import WaterIcon from '@/assets/icons/WaterIcon.svg'

const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

/** @type {{ dataSource: AiidaDataSource }} */
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
      <span class="icon">
        <ElectricityIcon v-if="icon === 'electricity'" />
        <HeatIcon v-if="icon === 'heat'" />
        <MeterIcon v-if="icon === 'meter'" />
        <WaterIcon v-if="icon === 'water'" />
      </span>
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
  background: white;
}

.headline {
  word-break: break-word;
}

.fields {
  display: grid;
  gap: 0.5rem;
  color: var(--eddie-grey-medium);
}

.fields > div {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 1rem;
}

.fields dd {
  text-align: right;
}

.fields > div:not(.button-field),
.fields > .button-field > dt {
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--eddie-grey-light);
  border-radius: var(--border-radius);
}

.header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1rem;
  align-items: end;
  color: var(--eddie-primary);
  margin-bottom: 1rem;
  font-weight: 600;
}

.icon {
  height: 2rem;
  width: 2rem;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px solid var(--eddie-grey-light);
  border-radius: var(--border-radius);
}

.actions {
  flex-grow: 1;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 1rem;
  margin-top: 0.75rem;
}
</style>

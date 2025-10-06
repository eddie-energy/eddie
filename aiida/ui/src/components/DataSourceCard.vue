<script setup lang="ts">
import { BASE_URL } from '@/api'
import Button from '@/components/Button.vue'
import TrashIcon from '@/assets/icons/TrashIcon.svg'
import PenIcon from '@/assets/icons/PenIcon.svg'
import DataSourceIcon from '@/components/DataSourceIcon.vue'
import StatusDotIcon from '@/assets/icons/StatusDotIcon.svg'
import ChevronDownIcon from '@/assets/icons/ChevronDownIcon.svg'
import { computed, ref } from 'vue'
import type { AiidaDataSource } from '@/types'
import { dataSourceImages } from '@/stores/dataSources'

const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

const { dataSource, startOpen } = defineProps<{
  dataSource: AiidaDataSource
  startOpen?: boolean
}>()
const isOpen = ref(startOpen)
const emit = defineEmits(['edit', 'delete', 'reset', 'enableToggle'])
//TODO see #GH-1957
const mqttCertificate = false

const {
  countryCode,
  asset,
  dataSourceType,
  enabled,
  id,
  mqttExternalHost,
  mqttSubscribeTopic,
  mqttUsername,
  name,
  simulationPeriod,
  icon,
} = dataSource

const image = computed(() => dataSourceImages.value[dataSource.id])
</script>

<template>
  <article class="card" :class="{ 'is-open': isOpen }">
    <header class="header" @click="isOpen = !isOpen">
      <DataSourceIcon :icon />
      <h2 class="heading-4 headline">{{ name }}</h2>
      <span class="text-xsmall data-source-type">{{ dataSourceType }}</span>
      <button class="chevron" aria-label="Open Data Source Card">
        <ChevronDownIcon />
      </button>
    </header>

    <img v-if="image" :src="image" alt="image for data source" role="presentation" class="image" />

    <dl class="fields" :class="{ 'with-image': image }">
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

      <template v-if="mqttExternalHost && mqttSubscribeTopic && mqttUsername">
        <div>
          <dt>MQTT Server URI</dt>
          <dd>{{ mqttExternalHost }}</dd>
        </div>
        <div>
          <dt>MQTT Topic</dt>
          <dd>{{ mqttSubscribeTopic }}</dd>
        </div>
        <div>
          <dt>MQTT Username</dt>
          <dd>{{ mqttUsername }}</dd>
        </div>
        <div class="button-field">
          <dt>MQTT Password</dt>
          <dd>
            <Button button-style="secondary" @click="emit('reset')">Reset password</Button>
          </dd>
        </div>
        <div class="button-field" v-if="mqttCertificate">
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

      <div class="button-field toggle">
        <dt>Enabled</dt>
        <button
          class="toggle-button"
          :class="{ enabled: enabled }"
          @click="emit('enableToggle')"
          :aria-label="`${enabled ? 'Disable' : 'Enable'} Data Source`"
        >
          <StatusDotIcon class="toggle-icon" />
        </button>
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
  padding: var(--spacing-md);
  gap: var(--spacing-md);
  background: linear-gradient(180deg, #ffffff 0%, rgba(255, 255, 255, 0.9) 100%);

  &.is-open {
    .fields {
      display: grid;
    }
    .actions {
      display: flex;
    }
    .image {
      display: block;
    }
    .chevron {
      transform: rotate(180deg);
    }
  }
}

.headline {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.data-source-type {
  display: none;
}

.image {
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: var(--border-radius);
}

.fields {
  display: grid;
  gap: var(--spacing-sm);
  color: var(--eddie-grey-medium);

  > div {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-xs);
    font-size: 1rem;
    line-height: 1.5;
  }

  dd {
    font-weight: 600;
    line-height: 1;
    color: var(--eddie-grey-medium);
  }
  > div:not(.button-field),
  > .button-field > dt {
    background-color: var(--light);
    padding: var(--spacing-xs) var(--spacing-sm);
    border: 1px solid var(--eddie-grey-medium);
    border-radius: var(--border-radius);
  }
}

div.button-field {
  dt {
    width: 100%;
  }

  dd,
  button:not(.toggle-button) {
    width: 100%;
    justify-content: center;
  }
}

.header {
  display: grid;
  align-items: center;
  grid-template-columns: auto 1fr auto;
  gap: var(--spacing-md);
  color: var(--eddie-primary);
  cursor: pointer;
  font-weight: 600;
}

.chevron {
  cursor: pointer;
  justify-self: end;
  margin-left: auto;
  padding: 0.5rem;
  transition: transform 0.3s ease-in-out;
}

.toggle-icon {
  position: absolute;
  margin: 0.2rem 0.75rem;
  top: 0;
  left: 0;
  transform: translateX(-0.25rem);
  transition:
    transform 0.3s ease-in-out,
    color 0.3s ease-in-out;
}

.toggle-button {
  position: relative;
  cursor: pointer;
  width: 3rem;
  height: 1.5rem;
  border-radius: 1rem;
  border: 1px solid var(--eddie-primary);
  background-color: var(--light);
  color: var(--eddie-primary);
  transition: background-color 0.3s ease-in-out;

  &:hover {
    .toggle-icon {
      transform: translateX(0.75rem);
    }
  }

  &.enabled {
    background-color: var(--eddie-primary);
    color: var(--light);
    .toggle-icon {
      transform: translateX(0.75rem);
    }

    &:hover {
      background-color: var(--light);
      color: var(--eddie-primary);
      .toggle-icon {
        transform: translateX(-0.2rem);
      }
    }
  }
}

.actions {
  flex-grow: 1;
  flex-direction: column;
  justify-content: space-between;
  align-items: end;
  gap: var(--spacing-md);

  button {
    width: 100%;
    justify-content: center;
  }
}

div.toggle {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
}

.image,
.fields,
.actions {
  display: none;
}

@media screen and (min-width: 640px) {
  div.button-field:not(.toggle) {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .actions {
    display: flex;
    flex-direction: row;
    grid-column: span 2;

    button {
      width: fit-content;
      justify-content: flex-start;
    }
  }

  .card {
    display: grid;
    grid-template-columns: 1fr 2fr;
    grid-template-rows: max-content;
    background-color: var(--light);
    padding: var(--spacing-xlg);
  }
  .header {
    grid-column: span 2;
  }
  .fields {
    display: grid;
    grid-column: span 2;
  }
  .with-image {
    grid-column: span 1;
  }
  .image {
    display: block;
  }
  .chevron {
    display: none;
  }
  .data-source-type {
    display: inline;
  }
}

@media screen and (min-width: 1620px) {
  .fields {
    > div {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: var(--spacing-md);
      align-items: center;
    }
    dd {
      text-align: right;
    }
  }
}
</style>

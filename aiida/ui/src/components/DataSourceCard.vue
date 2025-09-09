<script setup lang="ts">
import { BASE_URL } from '@/api'
import Button from '@/components/Button.vue'
import TrashIcon from '@/assets/icons/TrashIcon.svg'
import PenIcon from '@/assets/icons/PenIcon.svg'
import DataSourceIcon from '@/components/DataSourceIcon.vue'
import StatusDotIcon from '@/assets/icons/StatusDotIcon.svg'
import ChevronDownIcon from '@/assets/icons/ChevronDownIcon.svg'
import { ref } from 'vue'

const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

const { dataSource, startOpen } = defineProps<{
  dataSource: any
  startOpen?: boolean
}>()
const isOpen = ref(startOpen)

const emit = defineEmits(['edit', 'delete', 'reset', 'enableToggle'])

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
  <article class="card" :class="{ 'is-open': isOpen }">
    <header class="header" @click="isOpen = !isOpen">
      <DataSourceIcon :icon />
      <h2 class="heading-4 headline">{{ name }}</h2>
      <span class="text-xsmall data-source-type">{{ dataSourceType }}</span>
      <button class="chevron" aria-label="Open Data Source Card">
        <ChevronDownIcon />
      </button>
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

      <div class="toggle-field">
        <dt>Enabled</dt>
        <button
          class="toggle-button"
          :class="{ enabled: enabled }"
          @click="emit('enableToggle')"
          aria-label="Disable Data Source"
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
    .fields,
    .actions {
      width: 100%;
      position: unset;
      height: 100%;
      opacity: 1;
      transition: opacity 0.5s ease;
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

.fields {
  display: grid;
  height: 0;
  width: 0;
  opacity: 0;
  position: absolute;
  gap: var(--spacing-sm);
  color: var(--eddie-grey-medium);
}

.fields > div {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--spacing-xs);
  font-size: 1rem;
  line-height: 1.5;
}

div.button-field {
  dt {
    width: 100%;
  }

  dd,
  button {
    width: 100%;
    justify-content: center;
  }
}

.fields dd {
  font-weight: 600;
  line-height: 1;
  color: var(--eddie-grey-medium);
}

.fields > div:not(.button-field),
.fields > .button-field > dt {
  background-color: var(--light);
  padding: var(--spacing-xs) var(--spacing-sm);
  border: 1px solid var(--eddie-grey-light);
  border-radius: var(--border-radius);
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

div.toggle-field {
  display: grid;
  align-items: center;
  grid-template-columns: 1fr auto;
}

.toggle-icon {
  position: absolute;
  margin: 2px 4.5px;
  top: 0;
  left: 0;
  transition:
    transform 0.3s ease-in-out,
    color 0.3s ease-in-out;
}

.toggle-button {
  position: relative;
  cursor: pointer;
  width: 2rem;
  height: 1rem;
  border-radius: 1rem;
  border: 1px solid var(--eddie-primary);
  background-color: var(--light);
  color: var(--eddie-primary);
  transition: background-color 0.3s ease-in-out;

  &:hover {
    .toggle-icon {
      transform: translateX(13px);
    }
  }

  &.enabled {
    background-color: var(--eddie-primary);
    color: var(--light);
    .toggle-icon {
      transform: translateX(13px);
    }

    &:hover {
      background-color: var(--light);
      color: var(--eddie-primary);
      .toggle-icon {
        transform: translateX(0);
      }
    }
  }
}

.actions {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: end;
  gap: var(--spacing-md);
  position: absolute;
  opacity: 0;
  height: 0;
  width: 0;

  button {
    width: 100%;
    justify-content: center;
  }
}

@media screen and (min-width: 640px) {
  div.button-field {
    display: grid;
    grid-template-columns: 1fr 1fr;
    dd,
    button {
      width: 100%;
      justify-content: center;
    }
  }

  .actions {
    flex-direction: row;

    button {
      width: fit-content;
      justify-content: flex-start;
    }
  }
}

@media screen and (min-width: 1024px) {
  .card {
    background-color: var(--light);
  }
  .fields,
  .actions {
    position: unset;
    height: 100%;
    width: 100%;
    opacity: 1;
    transition: opacity 0.5s ease;
  }
  .chevron {
    display: none;
  }
  .data-source-type {
    display: inline;
  }
  .actions {
    display: flex;
  }
  .fields > div {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: var(--spacing-md);
    align-items: center;
  }
  .fields dd {
    text-align: right;
  }
}
</style>

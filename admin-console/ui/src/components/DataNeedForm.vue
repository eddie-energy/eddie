<script setup lang="ts">
import { ref } from 'vue'

import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import MultiSelect from 'primevue/multiselect'
import Button from 'primevue/button'
import { DATA_NEEDS_API_URL } from '@/config'
import { ASSETS, ENERGY_TYPES, GRANULARITIES, REGION_CONNECTORS, SCHEMAS } from '@/constants'
import type {
  AccountingPointDataNeed,
  AiidaDataNeed,
  DataNeed,
  DataNeedType,
  InboundAiidaDataNeed,
  OutboundAiidaDataNeed,
  ValidatedHistoricalDataDataNeed
} from '@/types'

const emit = defineEmits(['created'])

type DataNeedForm =
  | Omit<AccountingPointDataNeed, 'id' | 'createdAt'>
  | Omit<InboundAiidaDataNeed, 'id' | 'createdAt'>
  | Omit<OutboundAiidaDataNeed, 'id' | 'createdAt'>
  | Omit<ValidatedHistoricalDataDataNeed, 'id' | 'createdAt'>

type ValidatedFields = Omit<ValidatedHistoricalDataDataNeed, keyof DataNeed>
type AiidaFields = Omit<AiidaDataNeed, keyof DataNeed>

const commonDefaults: Omit<DataNeedForm, 'type'> = {
  name: '',
  description: '',
  purpose: '',
  policyLink: '',
  enabled: true,
  regionConnectorFilter: { type: 'blocklist', regionConnectorIds: [] }
}

const validatedDefaults: ValidatedFields = {
  duration: {
    type: 'relativeDuration'
  },
  energyType: 'ELECTRICITY',
  minGranularity: 'P1D',
  maxGranularity: 'P1D'
}

const aiidaDefaults: AiidaFields = {
  duration: {
    type: 'relativeDuration'
  },
  transmissionSchedule: '*/2 * * * * *',
  schemas: [],
  asset: 'CONNECTION-AGREEMENT-POINT',
  dataTags: []
}

const form = ref<DataNeedForm>({
  type: 'account',
  ...commonDefaults
})

const submitting = ref(false)
const message = ref<string | null>(null)

function onTypeChanged(value: DataNeedType) {
  const next = { ...commonDefaults, type: value }

  if (value === 'validated') {
    form.value = { ...next, ...validatedDefaults } as DataNeedForm
  }

  if (value === 'inbound-aiida' || value === 'outbound-aiida') {
    form.value = { ...next, ...aiidaDefaults } as DataNeedForm
  }
}

async function submitForm() {
  message.value = null

  submitting.value = true
  try {
    const response = await fetch(`${DATA_NEEDS_API_URL}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    if (!response.ok) {
      const text = await response.text()
      throw new Error(text || `HTTP ${response.status}`)
    }

    message.value = 'Data need created.'
    emit('created')
  } catch (err: any) {
    message.value = `Error: ${err.message}`
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <h2>Create Data Need</h2>

    <form @submit.prevent="submitForm">
      <div class="field">
        <label for="type">Type</label>
        <Select
          id="type"
          v-model="form.type"
          :options="[
            { label: 'Validated Historical Data', value: 'validated' },
            { label: 'Accounting Point', value: 'account' },
            { label: 'Inbound AIIDA', value: 'inbound-aiida' },
            { label: 'Outbound AIIDA', value: 'outbound-aiida' }
          ]"
          @valueChange="onTypeChanged"
          optionLabel="label"
          optionValue="value"
          required
        />
      </div>

      <div class="field">
        <label for="name">Name</label>
        <InputText id="name" v-model="form.name" required />
      </div>

      <div class="field">
        <label for="description">Description</label>
        <Textarea id="description" v-model="form.description" rows="3" autoResize required />
      </div>

      <div class="field">
        <label for="purpose">Purpose</label>
        <Textarea id="purpose" v-model="form.purpose" rows="3" autoResize required />
      </div>

      <div class="field">
        <label for="policy">Policy Link</label>
        <InputText id="policy" v-model="form.policyLink" type="url" required />
      </div>

      <div class="field">
        <label for="filterType">Region Filter Type</label>
        <Select
          id="filterType"
          v-model="form.regionConnectorFilter!.type"
          :options="[
            { label: 'Blocklist', value: 'blocklist' },
            { label: 'Allowlist', value: 'allowlist' }
          ]"
          optionLabel="label"
          optionValue="value"
          required
        />
      </div>

      <div class="field">
        <label for="regionIds">Region Connector IDs</label>
        <MultiSelect
          id="regionIds"
          v-model="form.regionConnectorFilter!.regionConnectorIds"
          :options="REGION_CONNECTORS"
          placeholder="Select region connectors"
        />
      </div>

      <template
        v-if="
          form.type === 'validated' ||
          form.type === 'inbound-aiida' ||
          form.type === 'outbound-aiida'
        "
      >
        <legend>Duration</legend>

        <div class="field">
          <label for="durationType">Duration Type</label>
          <Select
            id="durationType"
            v-model="form.duration.type"
            :options="[
              { label: 'Relative Duration', value: 'relativeDuration' },
              { label: 'Absolute Duration', value: 'absoluteDuration' }
            ]"
            optionLabel="label"
            optionValue="value"
            required
          />
        </div>

        <template v-if="form.duration.type === 'relativeDuration'">
          <div class="field">
            <label for="relStart">Relative Start (ISO, e.g. -P3M)</label>
            <InputText id="relStart" v-model="form.duration.start" />
          </div>
          <div class="field">
            <label for="relEnd">Relative End (ISO, e.g. P1Y)</label>
            <InputText id="relEnd" v-model="form.duration.end" />
          </div>
          <div class="field">
            <label for="sticky">Sticky Start Calendar Unit</label>
            <Select
              id="sticky"
              v-model="form.duration.stickyStartCalendarUnit"
              :options="['WEEK', 'MONTH', 'YEAR']"
            />
          </div>
        </template>

        <template v-if="form.duration.type === 'absoluteDuration'">
          <div class="field">
            <label for="absStart">Absolute Start</label>
            <InputText id="absStart" type="date" v-model="form.duration.start" required />
          </div>
          <div class="field">
            <label for="absEnd">Absolute End</label>
            <InputText id="absEnd" type="date" v-model="form.duration.end" required />
          </div>
        </template>
      </template>

      <template v-if="form.type === 'validated'">
        <div class="field">
          <label for="energyType">Energy Type</label>
          <Select id="energyType" v-model="form.energyType" :options="ENERGY_TYPES" required />
        </div>

        <div class="field">
          <label for="minGranularity">Min Granularity</label>
          <Select
            id="minGranularity"
            v-model="form.minGranularity"
            :options="GRANULARITIES"
            required
          />
        </div>

        <div class="field">
          <label for="maxGranularity">Max Granularity</label>
          <Select
            id="maxGranularity"
            v-model="form.maxGranularity"
            :options="GRANULARITIES"
            required
          />
        </div>
      </template>

      <template v-if="form.type === 'inbound-aiida' || form.type === 'outbound-aiida'">
        <div class="field">
          <label for="asset">Asset</label>
          <Select id="asset" v-model="form.asset" :options="ASSETS" required />
        </div>

        <div class="field">
          <label for="schedule">Transmission Schedule (Cron)</label>
          <InputText id="schedule" v-model="form.transmissionSchedule" required />
        </div>

        <div class="field">
          <label for="schemas">Schemas</label>
          <MultiSelect
            id="schemas"
            v-model="form.schemas"
            :options="SCHEMAS"
            placeholder="Select schemas"
          />
        </div>

        <div class="field">
          <label for="dataTags">Data Tags (comma separated)</label>
          <InputText
            id="dataTags"
            :value="form.dataTags?.join(', ')"
            @valueChange="
              (value: string) =>
                ((form as AiidaDataNeed).dataTags = value
                  .split(',')
                  .map((s) => s.trim())
                  .filter(Boolean))
            "
          />
        </div>
      </template>

      <Button type="submit" label="Create" :loading="submitting" />
    </form>

    <p v-if="message">{{ message }}</p>
  </div>
</template>

<style scoped>
form {
  display: grid;
  grid-template-columns: subgrid;
  justify-items: start;
  gap: 1rem;
}

.field {
  display: grid;
  justify-items: start;
}
</style>

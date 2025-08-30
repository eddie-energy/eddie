<script setup lang="ts">
import {
  addDataSource,
  getAssetTypes,
  getDataSourceTypes,
  getModbusDevices,
  getModbusModels,
  getModbusVendors,
  saveDataSource,
} from '@/api'
import { onMounted, ref, toRaw, useTemplateRef, watch } from 'vue'
import { fetchDataSources } from '@/stores/dataSources.js'
import type { AiidaDataSource } from '@/types'
import Button from '@/components/Button.vue'

const SUPPORTED_COUNTRY_CODES = ['AT', 'FR', 'NL']
const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

const form = useTemplateRef<HTMLFormElement>('form')

const dataSourceTypes = await getDataSourceTypes()
const assetTypes = (await getAssetTypes()).assets

const props = defineProps(['open', 'dataSource'])
const emit = defineEmits(['hide'])
const modbusVendors = ref([])
const modbusModels = ref<{ id: string; name: string; vendorId: string }[]>([])
const modbusDevices = ref<{ id: string; name: string; modelId: string }[]>([])

const dataSourceRef = ref<AiidaDataSource>({
  enabled: false,
  id: '',
  dataSourceType: '',
  asset: '',
  name: '',
  countryCode: '',
})

watch(props, async () => {
  dataSourceRef.value = props.dataSource ? { ...toRaw(props.dataSource) } : { enabled: true }

  if (dataSourceRef.value.modbusSettings) {
    const { modbusVendor, modbusModel } = dataSourceRef.value.modbusSettings

    if (!modbusVendors.value) {
      modbusVendors.value = await getModbusVendors()
    }
    modbusModels.value = await getModbusModels(modbusVendor)
    modbusDevices.value = modbusModel ? await getModbusDevices(modbusModel) : []
  }
})

async function handleDataSourceTypeSelect(event: Event) {
  const dataSourceType = (event.target as HTMLSelectElement).value
  dataSourceRef.value.dataSourceType = dataSourceType

  if (dataSourceType === 'MODBUS') {
    dataSourceRef.value.modbusSettings = {
      modbusIp: '',
      modbusVendor: '',
      modbusModel: '',
      modbusDevice: '',
    }
    modbusVendors.value = await getModbusVendors()
  } else {
    delete dataSourceRef.value.modbusSettings
  }
}

async function handleModbusVendorSelect(event: Event) {
  const vendor = (event.target as HTMLSelectElement).value
  if (dataSourceRef.value.modbusSettings) {
    dataSourceRef.value.modbusSettings = {
      modbusVendor: vendor,
      modbusModel: '',
      modbusDevice: '',
      modbusIp: '',
    }
    modbusModels.value = await getModbusModels(vendor)
  }
}

async function handleModbusModelSelect(event: Event) {
  const model = (event.target as HTMLSelectElement).value
  if (dataSourceRef.value.modbusSettings) {
    dataSourceRef.value.modbusSettings.modbusModel = model
    modbusDevices.value = await getModbusDevices(model)
    dataSourceRef.value.modbusSettings.modbusDevice = ''
  }
}

function hide(event: Event) {
  // avoid conflict with hide event from Shoelace's select element
  if (event.target === event.currentTarget) {
    emit('hide')
  }
}

function save(event: Event) {
  event.preventDefault()

  const promise = props.dataSource
    ? saveDataSource(props.dataSource.id, dataSourceRef.value)
    : addDataSource(dataSourceRef.value)

  promise.then(() => {
    fetchDataSources()
  })

  emit('hide')
}

onMounted(() => {
  // Prevent form from skipping validation in Shoelace elements
  customElements.whenDefined('sl-input').then(() => {
    form.value?.addEventListener('submit', save)
  })
})
</script>

<template>
  <sl-dialog :open="open || undefined" @sl-hide="hide">
    <h2 class="heading-2" slot="label">Add Data Source</h2>
    <form ref="form" id="data-source-form">
      <sl-input
        name="name"
        label="Name"
        required
        :value="dataSourceRef.name"
        @sl-input="dataSourceRef.name = $event.target.value"
      ></sl-input>
      <br />
      <sl-checkbox
        name="enabled"
        :checked="dataSourceRef.enabled"
        @sl-input="dataSourceRef.enabled = $event.target.checked"
      >
        Enabled
      </sl-checkbox>
      <br />
      <br />
      <sl-select
        name="countryCode"
        label="Country"
        required
        :value="dataSourceRef.countryCode"
        @sl-input="dataSourceRef.countryCode = $event.target.value"
      >
        <sl-option v-for="country in SUPPORTED_COUNTRY_CODES" :value="country" :key="country">
          {{ COUNTRY_NAMES.of(country) }}
        </sl-option>
      </sl-select>
      <br />
      <sl-select
        label="Asset Type"
        required
        :value="dataSourceRef.asset"
        @sl-input="dataSourceRef.asset = $event.target.value"
      >
        <sl-option v-for="type in assetTypes" :value="type" :key="type">
          {{ type }}
        </sl-option>
      </sl-select>
      <br />
      <sl-select
        name="dataSourceType"
        label="Data Source Type"
        required
        :value="dataSourceRef.dataSourceType"
        @sl-input="handleDataSourceTypeSelect"
      >
        <sl-option
          v-for="{ identifier, name } in dataSourceTypes"
          :value="identifier"
          :key="identifier"
        >
          {{ name }}
        </sl-option>
      </sl-select>
      <template v-if="dataSourceRef.dataSourceType === 'SIMULATION'">
        <br />
        <sl-input
          name="simulationPeriod"
          label="Simulation Period"
          type="number"
          required
          :value="dataSourceRef.simulationPeriod"
          @sl-input="dataSourceRef.simulationPeriod = $event.target.value"
        ></sl-input>
      </template>
      <template v-if="dataSourceRef.dataSourceType === 'MODBUS'">
        <br />
        <sl-input
          v-if="dataSourceRef.modbusSettings?.modbusIp"
          name="modbusIp"
          label="Local IP Address"
          placeholder="e.g. 192.168.x.x / localhost"
          required
          help-text="Enter a private local IP address (e.g. 192.168.x.x)"
          pattern="(?:localhost|((25[0-5]|2[0-4][0-9]|1\d{2}|[1-9]?\d)(\.)){3}(25[0-5]|2[0-4][0-9]|1\d{2}|[1-9]?\d))"
          :value="dataSourceRef.modbusSettings?.modbusIp"
          @sl-input="dataSourceRef.modbusSettings!.modbusIp = $event.target.value"
        ></sl-input>
        <br />
        <sl-select
          name="modbusVendor"
          label="Vendor"
          placeholder="Select a vendor..."
          required
          :value="dataSourceRef.modbusSettings?.modbusVendor"
          @sl-input="handleModbusVendorSelect"
        >
          <sl-option v-for="{ id, name } in modbusVendors" :value="id" :key="id">
            {{ name }}
          </sl-option>
        </sl-select>
        <br />
        <sl-select
          name="modbusModel"
          label="Model"
          placeholder="Select a model..."
          required
          :disabled="!dataSourceRef.modbusSettings?.modbusVendor"
          :value="dataSourceRef.modbusSettings?.modbusModel"
          @sl-input="handleModbusModelSelect"
        >
          <sl-option v-for="{ id, name } in modbusModels" :value="id" :key="id">
            {{ name }}
          </sl-option>
        </sl-select>
        <br />
        <sl-select
          v-if="dataSourceRef.modbusSettings?.modbusModel"
          id="modbus-device-list"
          name="modbusDevice"
          label="Device"
          placeholder="Select a device..."
          required
          :disabled="!dataSourceRef.modbusSettings?.modbusModel"
          :value="dataSourceRef.modbusSettings?.modbusDevice"
          @sl-input="dataSourceRef.modbusSettings!.modbusDevice = $event.target.value"
        >
          <sl-option v-for="{ id, name } in modbusDevices" :value="id" :key="id">
            {{ name }}
          </sl-option>
        </sl-select>
      </template>
    </form>

    <div class="actions">
      <Button type="submit" form="data-source-form">Save</Button>
      <Button button-style="error" type="button" @click="hide">Cancel</Button>
    </div>
  </sl-dialog>
</template>

<style scoped>
.actions {
  margin-top: 2rem;
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: end;
}
</style>

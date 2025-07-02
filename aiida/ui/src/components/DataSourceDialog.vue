<script setup>
import {
  addDataSource,
  getAssetTypes,
  getDataSourceTypes,
  getModbusDevices,
  getModbusModels,
  getModbusVendors,
  saveDataSource,
} from '@/api.js'
import { onMounted, ref, useTemplateRef, watch } from 'vue'

/** @type {ShallowRef<HTMLFormElement>} */
const form = useTemplateRef('form')

const dataSourceTypes = await getDataSourceTypes()
const assetTypes = (await getAssetTypes()).assets

const props = defineProps(['open', 'dataSource'])
const emit = defineEmits(['hide'])

/** @type {Ref<AiidaDataSource>} */
const dataSource = ref({ enabled: false })

watch(props, async () => {
  dataSource.value = props.dataSource ?? { enabled: true }

  if (dataSource.value.modbusSettings) {
    const { modbusVendor, modbusModel } = dataSource.value.modbusSettings

    if (!modbusVendors.value) {
      modbusVendors.value = await getModbusVendors()
    }
    modbusModels.value = await getModbusModels(modbusVendor)
    modbusDevices.value = await getModbusDevices(modbusModel)
  }
})

const modbusVendors = ref([])
const modbusModels = ref([])
const modbusDevices = ref([])

async function handleDataSourceTypeSelect(event) {
  const dataSourceType = event.target.value
  dataSource.value.dataSourceType = dataSourceType

  if (dataSourceType === 'MODBUS') {
    dataSource.value.modbusSettings = {}
    modbusVendors.value = await getModbusVendors()
  } else {
    delete dataSource.value.modbusSettings
  }
}

async function handleModbusVendorSelect(event) {
  const vendor = event.target.value
  dataSource.value.modbusSettings.modbusVendor = vendor
  modbusModels.value = await getModbusModels(vendor)
  delete dataSource.value.modbusSettings.modbusModel
  delete dataSource.value.modbusSettings.modbusDevice
}

async function handleModbusModelSelect(event) {
  const model = event.target.value
  dataSource.value.modbusSettings.modbusModel = model
  modbusDevices.value = await getModbusDevices(model)
  delete dataSource.value.modbusSettings.modbusDevice
}

function hide(event) {
  // avoid conflict with hide event from Shoelace's select element
  if (event.target === event.currentTarget) {
    emit('hide')
  }
}

function save() {
  if (props.dataSource) {
    return saveDataSource(props.dataSource.id, dataSource.value)
  }

  return addDataSource(dataSource.value)
}

onMounted(() => {
  // Prevent form from skipping validation in Shoelace elements
  customElements.whenDefined('sl-input').then(() => {
    form.value.addEventListener('submit', save)
  })
})
</script>

<template>
  <sl-dialog label="Add Data Source" :open="open || undefined" @sl-hide="hide">
    <form ref="form" id="data-source-form">
      <sl-input
        name="name"
        label="Name"
        required
        :value="dataSource.name"
        @sl-input="dataSource.name = $event.target.value"
      ></sl-input>
      <br />
      <sl-checkbox
        name="enabled"
        :checked="dataSource.enabled"
        @sl-input="dataSource.enabled = $event.target.checked"
      >
        Enabled
      </sl-checkbox>
      <br />
      <br />
      <sl-select
        label="Asset Type"
        required
        :value="dataSource.asset"
        @sl-input="dataSource.asset = $event.target.value"
      >
        <sl-option v-for="type in assetTypes" :value="type">
          {{ type }}
        </sl-option>
      </sl-select>
      <br />
      <sl-select
        name="dataSourceType"
        label="Data Source Type"
        required
        :value="dataSource.dataSourceType"
        @sl-input="handleDataSourceTypeSelect"
      >
        <sl-option v-for="{ identifier, name } in dataSourceTypes" :value="identifier">
          {{ name }}
        </sl-option>
      </sl-select>
      <template v-if="dataSource.dataSourceType === 'SIMULATION'">
        <br />
        <sl-input
          name="simulationPeriod"
          label="Simulation Period"
          type="number"
          required
          :value="dataSource.simulationPeriod"
          @sl-input="dataSource.simulationPeriod = $event.target.value"
        ></sl-input>
      </template>
      <template v-if="dataSource.dataSourceType === 'MODBUS'">
        <br />
        <sl-input
          name="modbusIp"
          label="Local IP Address"
          placeholder="e.g. 192.168.x.x / localhost"
          required
          help-text="Enter a private local IP address (e.g. 192.168.x.x)"
          pattern="(?:localhost|((25[0-5]|2[0-4][0-9]|1\d{2}|[1-9]?\d)(\.)){3}(25[0-5]|2[0-4][0-9]|1\d{2}|[1-9]?\d))"
          :value="dataSource.modbusSettings?.modbusIp"
          @sl-input="dataSource.modbusSettings.modbusIp = $event.target.value"
        ></sl-input>
        <br />
        <sl-select
          name="modbusVendor"
          label="Vendor"
          placeholder="Select a vendor..."
          required
          :value="dataSource.modbusSettings?.modbusVendor"
          @sl-input="handleModbusVendorSelect"
        >
          <sl-option v-for="{ id, name } in modbusVendors" :value="id">
            {{ name }}
          </sl-option>
        </sl-select>
        <br />
        <sl-select
          name="modbusModel"
          label="Model"
          placeholder="Select a model..."
          required
          :disabled="!dataSource.modbusSettings?.modbusVendor"
          :value="dataSource.modbusSettings?.modbusModel"
          @sl-input="handleModbusModelSelect"
        >
          <sl-option v-for="{ id, name } in modbusModels" :value="id">
            {{ name }}
          </sl-option>
        </sl-select>
        <br />
        <sl-select
          id="modbus-device-list"
          name="modbusDevice"
          label="Device"
          placeholder="Select a device..."
          required
          :disabled="!dataSource.modbusSettings?.modbusModel"
          :value="dataSource.modbusSettings?.modbusDevice"
          @sl-input="dataSource.modbusSettings.modbusDevice = $event.target.value"
        >
          <sl-option v-for="{ id, name } in modbusDevices" :value="id">
            {{ name }}
          </sl-option>
        </sl-select>
      </template>
    </form>

    <sl-button slot="footer" type="submit" variant="primary" form="data-source-form"
      >Save
    </sl-button>
    <sl-button slot="footer" type="button" variant="neutral" @click="hide">Cancel </sl-button>
  </sl-dialog>
</template>

<style scoped></style>

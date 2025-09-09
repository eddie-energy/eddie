<script setup lang="ts">
import ModalDialog from '@/components/ModalDialog.vue'
import Button from '../Button.vue'
import { computed, onMounted, ref, useTemplateRef, watch } from 'vue'
import CustomSelect from '@/components/CustomSelect.vue'
import ImageUploadField from '../ImageUploadField.vue'
import ElectricityIcon from '@/assets/icons/ElectricityIcon.svg'
import MeterIcon from '@/assets/icons/MeterIcon.svg'
import WaterIcon from '@/assets/icons/WaterIcon.svg'
import HeatIcon from '@/assets/icons/HeatIcon.svg'
import CheckmarkIcon from '@/assets/icons/CheckmarkIcon.svg'
import {
  addDataSource,
  getAssetTypes,
  getDataSourceTypes,
  getModbusDevices,
  getModbusModels,
  getModbusVendors,
  saveDataSource,
} from '@/api'
import type { AiidaDataSource } from '@/types'
import { fetchDataSources } from '@/stores/dataSources'

const getEmptyDataSource = () => {
  return {
    name: '',
    asset: '',
    dataSourceType: '',
    enabled: true,
    id: '',
    countryCode: '',
    modbusSettings: {
      modbusIp: '',
      modbusVendor: '',
      modbusModel: '',
      modbusDevice: '',
    },
    simulationPeriod: 0,
  }
}

const SUPPORTED_COUNTRY_CODES = ['AT', 'FR', 'NL']
const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

const countryOptions = SUPPORTED_COUNTRY_CODES.map((country) => {
  return {
    label: COUNTRY_NAMES.of(country),
    value: country,
  }
})

const modal = useTemplateRef('dataSourceModal')
const formRef = useTemplateRef('form')
const title = ref('Add Data Source')
const errors = ref<Record<string, string>>({})
const dataSource = ref<AiidaDataSource>(getEmptyDataSource())
const dataSourceTypeOptions = ref<{ label: string; value: string }[]>([])
const assetTypeOptions = ref<{ label: string; value: string }[]>([])
const modbusVendorsOptions = ref<{ label: string; value: string }[]>([])
const modbusModelsOptions = ref<{ label: string; value: string }[]>([])
const modbusDevicesOptions = ref<{ label: string; value: string }[]>([])
const imageFile = ref<File | null>(null)
const icon = ref<'electricity' | 'heat' | 'meter' | 'water' | ''>('')
const loading = ref(false)

const getInitalFormData = (data?: AiidaDataSource) => {
  if (data) {
    dataSource.value = data
    title.value = 'Edit Data Source'
  } else {
    dataSource.value = getEmptyDataSource()
    title.value = 'Add Data Source'
  }
}

const dataSourceIcons = {
  electricity: ElectricityIcon,
  heat: HeatIcon,
  meter: MeterIcon,
  water: WaterIcon,
}

onMounted(async () => {
  dataSourceTypeOptions.value = (await getDataSourceTypes()).map((type) => {
    return {
      label: type.name,
      value: type.identifier,
    }
  })
  assetTypeOptions.value = (await getAssetTypes()).assets.map((asset: string) => ({
    label: asset,
    value: asset,
  }))
})

const dataSourceType = computed(() => dataSource.value.dataSourceType)
const vendor = computed(() => dataSource.value.modbusSettings?.modbusVendor)
const model = computed(() => dataSource.value.modbusSettings?.modbusModel)

watch(
  [dataSourceType, vendor, model],
  async ([newDataSourceType, newVendor, newModel], [, oldVendor, oldModel]) => {
    if (newDataSourceType === 'MODBUS') {
      modbusVendorsOptions.value = (await getModbusVendors()).map((vend) => {
        return {
          label: vend.name,
          value: vend.id,
        }
      })
      if (dataSource.value.modbusSettings) {
        if (newVendor && newVendor !== oldVendor) {
          dataSource.value.modbusSettings.modbusModel = ''
          dataSource.value.modbusSettings.modbusDevice = ''
          modbusModelsOptions.value = (await getModbusModels(newVendor)).map((mod) => {
            return {
              label: mod.name,
              value: mod.id,
            }
          })
        }
        if (newModel && newModel !== oldModel) {
          dataSource.value.modbusSettings.modbusDevice = ''
          modbusDevicesOptions.value = (await getModbusDevices(newModel)).map((dev) => {
            return {
              label: dev.name,
              value: dev.id,
            }
          })
        }
      }
    }
  },
)

const handleRequired = (value: string | number | undefined, label: string, key: string) => {
  if (!value) {
    errors.value[key] = `* ${label} is a required field`
  }
}

const validateForm = () => {
  errors.value = {}
  const requiredFields = [
    { value: dataSource.value?.name, label: 'Name', key: 'name' },
    { value: dataSource.value?.asset, label: 'Asset Type', key: 'assetType' },
    { value: dataSource.value?.dataSourceType, label: 'Data Source Type', key: 'dataSourceType' },
    { value: dataSource.value?.countryCode, label: 'Country', key: 'country' },
    //{ value: dataSource.value?.icon, label: 'Data Source Icon', key: 'icon' },
  ]
  requiredFields.forEach((field) => handleRequired(field.value, field.label, field.key))

  if (dataSource.value?.dataSourceType === 'MODBUS') {
    const modBusRequiredFields = [
      { value: dataSource.value.modbusSettings?.modbusIp, label: 'IP Address', key: 'ipAddress' },
      { value: dataSource.value.modbusSettings?.modbusVendor, label: 'Vendor', key: 'vendor' },
      { value: dataSource.value.modbusSettings?.modbusModel, label: 'Model', key: 'model' },
      { value: dataSource.value.modbusSettings?.modbusDevice, label: 'Device', key: 'device' },
    ]
    modBusRequiredFields.forEach((field) => handleRequired(field.value, field.label, field.key))
  }
  if (dataSource.value?.dataSourceType === 'SIMULATION') {
    handleRequired(dataSource.value?.simulationPeriod, 'Simulation Period', 'simPeriod')
  }
  if (imageFile.value && !imageFile.value.type.startsWith('image/')) {
    errors.value['image'] = 'Uploaded file must be an image.'
  } else if (imageFile.value && imageFile.value.size > 20 * 1024 * 1024) {
    errors.value['image'] = 'Image size must be less than 20MB.'
  }
}

const handleFormSubmit = async () => {
  if (formRef.value && dataSource.value) {
    validateForm()
    if (!Object.keys(errors.value).length) {
      loading.value = true
      try {
        if (dataSource.value.id) {
          await saveDataSource(dataSource.value.id, {
            ...dataSource.value,
          })
        } else {
          await addDataSource(dataSource.value)
        }
        await fetchDataSources()
        loading.value = false
        modal.value?.close()
      } catch {
        loading.value = false
        modal.value?.close()
      }
    }
  }
}

const showModal = (data?: AiidaDataSource) => {
  getInitalFormData(data)
  modal.value?.showModal()
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog
    :title
    ref="dataSourceModal"
    class="data-source-dialog"
    :class="{ 'is-loading': loading }"
  >
    <form
      ref="form"
      novalidate
      class="data-source-form"
      :class="{ 'columns-3': dataSource?.dataSourceType === 'MODBUS' }"
      @submit.prevent="handleFormSubmit"
    >
      <div class="column">
        <div class="input-field">
          <label for="name"> Name </label>
          <input
            placeholder="Data Source Name"
            required
            type="text"
            id="name"
            v-model="dataSource.name"
            name="name"
          />
          <p v-if="errors['name']" class="error-message">{{ errors['name'] }}</p>
        </div>
        <div class="checkbox-field">
          <label for="enable">Enabled</label>
          <input
            type="checkbox"
            id="enable"
            name="enable"
            required
            v-model="dataSource.enabled"
            class="checkbox-input"
          />
          <CheckmarkIcon class="checkbox-icon" />
        </div>
        <div class="input-field">
          <label for="assetType"> Asset Type </label>
          <CustomSelect
            id="assetType"
            v-model="dataSource.asset"
            placeholder="Your Asset Type"
            :options="assetTypeOptions"
            :name="'assetType'"
            required
          />
          <p v-if="errors['assetType']" class="error-message">{{ errors['assetType'] }}</p>
        </div>
        <div class="input-field">
          <label for="datasourceType">Data Source Type </label>
          <CustomSelect
            id="datasourceType"
            v-model="dataSource.dataSourceType"
            placeholder="Your Data Source Type"
            name="datasourceType"
            required
            :options="dataSourceTypeOptions"
          />
          <p v-if="errors['datasourceType']" class="error-message">
            {{ errors['datasourceType'] }}
          </p>
        </div>
        <div class="input-field">
          <label for="country">Country </label>
          <CustomSelect
            id="country"
            v-model="dataSource.countryCode"
            placeholder="Select Country"
            name="country"
            required
            :options="countryOptions"
          />
          <p v-if="errors['country']" class="error-message">
            {{ errors['country'] }}
          </p>
        </div>
      </div>
      <Transition name="extra-column">
        <div
          class="column"
          v-if="
            dataSource.dataSourceType === 'MODBUS' || dataSource.dataSourceType === 'SIMULATION'
          "
        >
          <template v-if="dataSource.dataSourceType === 'MODBUS' && dataSource.modbusSettings">
            <div class="input-field extra-margin">
              <label for="ipAddress"> Local IP Address</label>
              <input
                id="ipAddress"
                v-model="dataSource.modbusSettings.modbusIp"
                name="ipAddress"
                required
              />
              <p v-if="errors['ipAddress']" class="error-message">{{ errors['ipAddress'] }}</p>
            </div>
            <div class="input-field">
              <label for="vendor"> Vendor </label>
              <CustomSelect
                id="vendor"
                v-model="dataSource.modbusSettings.modbusVendor"
                placeholder="Select Vendor"
                :options="modbusVendorsOptions"
                name="vendor"
                required
              />
              <p v-if="errors['vendor']" class="error-message">{{ errors['vendor'] }}</p>
            </div>
            <div class="input-field">
              <label for="model"> Model </label>
              <CustomSelect
                id="model"
                v-model="dataSource.modbusSettings.modbusModel"
                placeholder="Select Model"
                :options="modbusModelsOptions"
                :disabled="!dataSource.modbusSettings.modbusVendor"
                name="model"
                required
              />
              <p v-if="errors['model']" class="error-message">{{ errors['model'] }}</p>
            </div>
            <div class="input-field">
              <label for="device"> Device </label>
              <CustomSelect
                id="device"
                v-model="dataSource.modbusSettings.modbusDevice"
                placeholder="Select Device"
                :options="modbusDevicesOptions"
                :disabled="!dataSource.modbusSettings.modbusModel"
                name="device"
                required
              />
              <p v-if="errors['device']" class="error-message">{{ errors['device'] }}</p>
            </div>
          </template>
          <template v-else>
            <div class="input-field">
              <label for="simPeriod"> Simulation Period </label>
              <input
                placeholder="Simulation Period"
                required
                type="number"
                id="simPeriod"
                v-model="dataSource.simulationPeriod"
                min="0"
                name="simPeriod"
              />
              <p v-if="errors['simPeriod']" class="error-message">{{ errors['simPeriod'] }}</p>
            </div>
          </template>
        </div>
      </Transition>
      <div class="column last-column">
        <div class="input-field extra-margin">
          <label>Choose an Icon</label>
          <div class="icon-select">
            <button
              v-for="(dataIcon, key) in dataSourceIcons"
              :key
              @click.prevent="icon = key"
              class="icon-button"
              :class="{ selected: icon === key }"
            >
              <component :is="dataIcon"></component>
            </button>
          </div>
          <p v-if="errors['icon']" class="error-message">{{ errors['icon'] }}</p>
        </div>
        <div class="input-field">
          <p class="label">Upload Image</p>
          <ImageUploadField v-model="imageFile" />
          <p v-if="errors['image']" class="error-message">{{ errors['image'] }}</p>
        </div>
      </div>
    </form>
    <p class="info-text">Fields marked with * are required!</p>
    <div class="action-buttons">
      <Button button-style="error-secondary" @click="modal?.close()">Cancel</Button>
      <Button @click="formRef?.requestSubmit()">Add</Button>
    </div>
    <div v-if="loading" class="loading-indicator"></div>
  </ModalDialog>
</template>

<style scoped>
.data-source-dialog {
  width: fit-content;
}
.is-loading {
  form,
  p,
  div:not(.loading-indicator) {
    opacity: 0;
  }
}
.data-source-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);

  label::after {
    content: ' *';
  }

  input {
    padding: var(--spacing-sm);
    border-radius: var(--border-radius);
    border: 1px solid var(--eddie-grey-medium);
  }
}
.checkbox-field {
  display: flex;
  position: relative;
  flex-direction: row-reverse;
  justify-content: flex-end;
  align-items: center;
  height: var(--spacing-xlg);
  gap: var(--spacing-md);

  input[type='checkbox'] {
    appearance: none;
    width: var(--spacing-xlg);
    height: var(--spacing-xlg);
    cursor: pointer;
    position: relative;
    &:checked {
      background-color: var(--eddie-primary);
    }
  }
}

.checkbox-icon {
  pointer-events: none;
  position: absolute;
  color: var(--light);
  top: 50%;
  transform: translateY(-50%);
  margin-left: 4px;
}

.column {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xlg);
}
.input-field {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}
.icon-select {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}
.icon-button {
  border-radius: var(--border-radius);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border: 1px solid var(--eddie-primary);
  color: var(--eddie-primary);

  transition:
    color 0.3s ease-in-out,
    background-color 0.3s ease-in-out;

  &:hover,
  &.selected {
    background-color: var(--eddie-primary);
    color: var(--light);
  }
}

.info-text {
  margin: var(--spacing-xlg) 0;
}

.action-buttons {
  display: flex;
  width: 100%;
  justify-content: space-between;
}

.last-column {
  justify-content: space-between;
}

.extra-column-enter-active,
.extra-column-leave-active {
  transition: opacity 0.3s ease;
}

.extra-column-enter-from,
.extra-column-leave-to {
  opacity: 0;
}

@media screen and (min-width: 1024px) {
  .data-source-form {
    flex-direction: row;
  }
  .column {
    width: 20vw;
  }

  .extra-margin {
    margin-bottom: calc(var(--spacing-xlg) * 2);
  }
}
</style>

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
  addDataSourceImage,
  getAssetTypes,
  getDataSourceTypes,
  getModbusDevices,
  getModbusModels,
  getModbusVendors,
  saveDataSource,
} from '@/api'
import type { AiidaDataSource, AiidaDataSourceIcon } from '@/types'
import { fetchDataSources } from '@/stores/dataSources'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const getEmptyDataSource = (): AiidaDataSource => {
  return {
    name: '',
    asset: '',
    dataSourceType: '',
    enabled: true,
    id: '' as AiidaDataSourceIcon,
    countryCode: '',
    modbusIp: '',
    modbusVendor: '',
    modbusModel: '',
    modbusDevice: '',
    icon: '' as AiidaDataSourceIcon,
  }
}
// All EU countries
const SUPPORTED_COUNTRY_CODES = [
  'AT',
  'BE',
  'BG',
  'HR',
  'CY',
  'CZ',
  'DK',
  'EE',
  'FI',
  'FR',
  'DE',
  'GR',
  'HU',
  'IE',
  'IT',
  'LV',
  'LT',
  'LU',
  'MT',
  'NL',
  'PL',
  'PT',
  'RO',
  'SK',
  'SI',
  'ES',
  'SE',
]

const countryOptions = computed(() => {
  const country_names = new Intl.DisplayNames(locale.value, { type: 'region' })
  return SUPPORTED_COUNTRY_CODES.map((country) => {
    return {
      label: country_names.of(country),
      value: country,
    }
  })
})

const modal = useTemplateRef('dataSourceModal')
const formRef = useTemplateRef('form')
const operationType = ref('Add')
const errors = ref<Record<string, string>>({})
const dataSource = ref<AiidaDataSource>(getEmptyDataSource())
const dataSourceTypeOptions = ref<{ label: string; value: string }[]>([])
const assetTypeOptions = ref<{ label: string; value: string }[]>([])
const modbusVendorsOptions = ref<{ label: string; value: string }[]>([])
const modbusModelsOptions = ref<{ label: string; value: string }[]>([])
const modbusDevicesOptions = ref<{ label: string; value: string }[]>([])
const imageFile = ref<File | null>(null)
const loading = ref(false)
const emit = defineEmits(['showMqtt'])
const nonMQTTDataSourceTypes = ['SIMULATION', 'MODBUS']
const dataSourceTypesWithExtraField = ['SIMULATION', 'MODBUS', 'SINAPSI_ALFA']

const getInitalFormData = (data?: AiidaDataSource) => {
  if (data) {
    dataSource.value = data
    operationType.value = 'Edit'
  } else {
    dataSource.value = getEmptyDataSource()
    operationType.value = 'Add'
    imageFile.value = null
  }
}

const dataSourceIcons = {
  ELECTRICITY: ElectricityIcon,
  HEAT: HeatIcon,
  METER: MeterIcon,
  WATER: WaterIcon,
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
const vendor = computed(() => dataSource.value.modbusVendor)
const model = computed(() => dataSource.value.modbusModel)

watch(
  [dataSourceType, vendor, model],
  async ([newDataSourceType, newVendor, newModel], [, oldVendor, oldModel]) => {
    if (newDataSourceType !== 'MODBUS') {
      return
    }
    modbusVendorsOptions.value = (await getModbusVendors()).map((vend) => {
      return {
        label: vend.name,
        value: vend.id,
      }
    })
    if (newVendor && newVendor !== oldVendor) {
      dataSource.value.modbusModel = ''
      dataSource.value.modbusDevice = ''
      modbusModelsOptions.value = (await getModbusModels(newVendor)).map((mod) => {
        return {
          label: mod.name,
          value: mod.id,
        }
      })
    }
    if (newModel && newModel !== oldModel) {
      dataSource.value.modbusDevice = ''
      modbusDevicesOptions.value = (await getModbusDevices(newModel)).map((dev) => {
        return {
          label: dev.name,
          value: dev.id,
        }
      })
    }
  },
)

const handleRequired = (value: string | number | undefined, label: string, key: string) => {
  if (!value) {
    errors.value[key] = `* ${label} ${t('datasources.modal.isRequired')}`
  }
}

const validateForm = () => {
  errors.value = {}
  const requiredFields = [
    { value: dataSource.value?.name, label: t('datasources.modal.name'), key: 'name' },
    { value: dataSource.value?.asset, label: t('datasources.modal.assetType'), key: 'assetType' },
    {
      value: dataSource.value?.dataSourceType,
      label: t('datasources.modal.datasourceType'),
      key: 'dataSourceType',
    },
    { value: dataSource.value?.countryCode, label: t('datasources.modal.country'), key: 'country' },
    { value: dataSource.value?.icon, label: t('datasources.modal.icon'), key: 'icon' },
  ]
  requiredFields.forEach((field) => handleRequired(field.value, field.label, field.key))

  if (dataSource.value?.dataSourceType === 'MODBUS') {
    const modBusRequiredFields = [
      {
        value: dataSource.value.modbusIp,
        label: t('datasources.modal.localIpInput'),
        key: 'ipAddress',
      },
      {
        value: dataSource.value.modbusVendor,
        label: t('datasources.modal.vendorInput'),
        key: 'vendor',
      },
      {
        value: dataSource.value.modbusModel,
        label: t('datasources.modal.modelInput'),
        key: 'model',
      },
      {
        value: dataSource.value.modbusDevice,
        label: t('datasources.modal.deviceInput'),
        key: 'device',
      },
    ]
    modBusRequiredFields.forEach((field) => handleRequired(field.value, field.label, field.key))
  }
  if (dataSource.value?.dataSourceType === 'SIMULATION') {
    handleRequired(
      dataSource.value?.pollingInterval,
      t('datasources.modal.pollingInterval'),
      'pollInterval',
    )
  }
  if (dataSource.value?.dataSourceType === 'SINAPSI_ALFA') {
    handleRequired(
      dataSource.value?.activationKey,
      t('datasources.modal.acitvationKey'),
      'activationKey',
    )
  }
  if (imageFile.value && !imageFile.value.type.startsWith('image/')) {
    errors.value['image'] = t('datasources.modal.uploadImageError')
  } else if (imageFile.value && imageFile.value.size > 20 * 1024 * 1024) {
    errors.value['image'] = t('datasources.modal.uploadImageSize')
  }
}

const handleFormSubmit = async () => {
  if (!(formRef.value && dataSource.value)) {
    return
  }
  validateForm()
  if (!Object.keys(errors.value).length) {
    loading.value = true
    try {
      if (dataSource.value.id) {
        await saveDataSource(dataSource.value.id, {
          ...dataSource.value,
        })
      } else if (dataSource.value.dataSourceType === 'MODBUS') {
        const { dataSourceId } = await addDataSource(dataSource.value)
        dataSource.value.id = dataSourceId
      } else {
        const { dataSourceId, plaintextPassword } = await addDataSource(dataSource.value)
        dataSource.value.id = dataSourceId
        if (
          !nonMQTTDataSourceTypes.includes(dataSource.value.dataSourceType) &&
          plaintextPassword
        ) {
          emit('showMqtt', plaintextPassword)
        }
      }
      if (imageFile.value) {
        await addDataSourceImage(dataSource.value.id, imageFile.value)
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

const showModal = (data?: AiidaDataSource) => {
  getInitalFormData(data)
  modal.value?.showModal()
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog
    :title="`${operationType === 'Add' ? t('datasources.addButton') : t('datasources.editButton')}`"
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
          <label for="name"> {{ t('datasources.modal.name') }} </label>
          <input
            :placeholder="t('datasources.modal.namePlaceholder')"
            required
            type="text"
            id="name"
            v-model="dataSource.name"
            name="name"
            autocomplete="off"
          />
          <p v-if="errors['name']" class="error-message">{{ errors['name'] }}</p>
        </div>
        <div class="checkbox-field">
          <label for="enable">{{ t('datasources.modal.enabled') }}</label>
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
          <label id="assetType">{{ t('datasources.modal.assetType') }}</label>
          <CustomSelect
            v-model="dataSource.asset"
            :placeholder="t('datasources.modal.assetTypePlaceholder')"
            :options="assetTypeOptions"
            :name="'assetType'"
            required
            aria-labelledby="assetType"
          />
          <p v-if="errors['assetType']" class="error-message">{{ errors['assetType'] }}</p>
        </div>
        <div class="input-field">
          <label id="datasourceType">{{ t('datasources.modal.dataSourceType') }}</label>
          <CustomSelect
            v-model="dataSource.dataSourceType"
            :placeholder="t('datasources.modal.dataSourceTypePlaceholder')"
            name="datasourceType"
            required
            :options="dataSourceTypeOptions"
            aria-labelledby="datasourceType"
          />
          <p v-if="errors['datasourceType']" class="error-message">
            {{ errors['datasourceType'] }}
          </p>
        </div>
        <div class="input-field">
          <label id="country">{{ t('datasources.modal.country') }} </label>
          <CustomSelect
            v-model="dataSource.countryCode"
            :placeholder="t('datasources.modal.countryPlaceholder')"
            name="country"
            required
            :options="countryOptions"
            aria-labelledby="country"
          />
          <p v-if="errors['country']" class="error-message">
            {{ errors['country'] }}
          </p>
        </div>
      </div>
      <Transition name="extra-column">
        <div
          class="column"
          v-if="dataSourceTypesWithExtraField.includes(dataSource.dataSourceType)"
        >
          <template v-if="dataSource.dataSourceType === 'MODBUS'">
            <div class="input-field extra-margin">
              <label for="ipAddress"> {{ t('datasources.modal.localIpInput') }}</label>
              <input
                id="ipAddress"
                v-model="dataSource.modbusIp"
                name="ipAddress"
                placeholder="10.236.46.120"
                required
              />
              <p v-if="errors['ipAddress']" class="error-message">{{ errors['ipAddress'] }}</p>
            </div>
            <div class="input-field">
              <label for="vendor"> {{ t('datasources.modal.vendorInput') }} </label>
              <CustomSelect
                id="vendor"
                v-model="dataSource.modbusVendor"
                :placeholder="t('datasources.modal.vendorInputPlaceholder')"
                :options="modbusVendorsOptions"
                name="vendor"
                required
              />
              <p v-if="errors['vendor']" class="error-message">{{ errors['vendor'] }}</p>
            </div>
            <div class="input-field">
              <label for="model"> {{ t('datasources.modal.modelInput') }} </label>
              <CustomSelect
                id="model"
                v-model="dataSource.modbusModel"
                placeholder="t('datasources.modal.modelPlaceholder') "
                :options="modbusModelsOptions"
                :disabled="!dataSource.modbusVendor"
                name="model"
                required
              />
              <p v-if="errors['model']" class="error-message">{{ errors['model'] }}</p>
            </div>
            <div class="input-field">
              <label for="device"> {{ t('datasources.modal.deviceInput') }} </label>
              <CustomSelect
                id="device"
                v-model="dataSource.modbusDevice"
                :placeholder="t('datasources.modal.devicePlaceholder')"
                :options="modbusDevicesOptions"
                :disabled="!dataSource.modbusModel"
                name="device"
                required
              />
              <p v-if="errors['device']" class="error-message">{{ errors['device'] }}</p>
            </div>
          </template>
          <template v-if="dataSource.dataSourceType === 'SIMULATION'">
            <div class="input-field">
              <label for="pollInterval"> {{ t('datasources.modal.pollingInterval') }} </label>
              <input
                :placeholder="t('datasources.modal.pollingInterval')"
                required
                type="number"
                id="pollInterval"
                v-model="dataSource.pollingInterval"
                min="0"
                name="pollInterval"
              />
              <p v-if="errors['pollInterval']" class="error-message">
                {{ errors['pollInterval'] }}
              </p>
            </div>
          </template>
          <template v-if="dataSource.dataSourceType === 'SINAPSI_ALFA'">
            <div class="input-field">
              <label for="activationKey"> Sinapsi {{ t('datasources.modal.activationKey') }}</label>
              <input
                :placeholder="t('datasources.modal.activationKey')"
                required
                type="text"
                id="activationKey"
                v-model="dataSource.activationKey"
                name="activationKey"
              />
              <p v-if="errors['activationKey']" class="error-message">
                {{ errors['activationKey'] }}
              </p>
            </div>
          </template>
        </div>
      </Transition>
      <div class="column last-column">
        <div class="input-field extra-margin">
          <label>{{ t('datasources.modal.chooseIcon') }}</label>
          <div class="icon-select">
            <button
              v-for="(dataIcon, key) in dataSourceIcons"
              :key
              @click.prevent="dataSource.icon = key"
              class="icon-button"
              :class="{ selected: dataSource.icon === key }"
            >
              <component :is="dataIcon" />
            </button>
          </div>
          <p v-if="errors['icon']" class="error-message">{{ errors['icon'] }}</p>
        </div>
        <div class="input-field">
          <p class="label">{{ t('datasources.modal.uploadImage') }}</p>
          <ImageUploadField v-model="imageFile" />
          <p v-if="errors['image']" class="error-message">{{ errors['image'] }}</p>
        </div>
      </div>
    </form>
    <p class="info-text">{{ t('datasources.modal.requiredFields') }}</p>
    <div class="action-buttons">
      <Button button-style="error-secondary" @click="modal?.close()">
        {{ t('cancelButton') }}
      </Button>
      <Button @click="formRef?.requestSubmit()">
        {{ operationType === 'Edit' ? t('editButton') : t('addButton') }}
      </Button>
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
    &:not([type='checkbox']) {
      padding: var(--spacing-sm) var(--spacing-md);
    }

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
  margin-left: var(--spacing-xs);
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

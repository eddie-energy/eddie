# AIIDA UI

The information here is near identical to the [documentation](https://architecture.eddie.energy/aiida/1-running/ui.html), but offers a better developer experience.
It provides a quick overview offer to most important parts of the AIIDA UI.

The AIIDA UI is written in the Vue framework with the Composition API using TypeScript. If you are unfamiliar with Vue check out the official [docs](https://vuejs.org/guide/introduction.html). We also use [vue-router](https://router.vuejs.org/) for client side navigation.

## Running AIIDA UI Locally

Assuming you have the backend running as described in the [AIIDA README](../README.md), you can simply start the development server by running:

```sh
pnpm dev
```

Also double check variables defined in the [.env](.env) file as the URLs should match with your local backend.
If everything is setup correctly you should be able to navigate to [http://localhost:5173/](http://localhost:5173/) and see the working UI.

>[!WARNING]
> You can also start it without having the backend running, however you will just not be able to add / retrieve any permissions or datasources.

## Permissions

All components directly related to permissions are prefixed with `Permission`. Since we use permissions across multiple components we manage the permissions array in one place via a store [src/store/permissions.ts](./src/stores/permissions.ts).

### PermissionView

The view for the permission page. Contains the [AddPermissionModal](#addpermissionmodal) as well as the [PermissionList](#permissionlist).

### PermissionList

Component that manages a list of [PermissionDropdown](#permissiondropdown) components. Also handles all of the filtering logic for inbound and outbound permissions as well as the active, pending and complete states.
This component fetches all permissions on mount and passes a permission object to the subcomponent so we do not have to refetch the permission.

### PermissionDropdown

Displays a simplified version of a permission with name, date and status. Can be expanded to show the [PermissionDetails](#permissiondetails).

### PermissionDetails

Displays all information contained within a permission object. Depending on permission type some of the displayed fields change. E.g. inbound permission have an API key with a tooltip.
Also handles revoking / continuing a permission request. This component is also used in the UpdatePermissionModal to show permission details of a newly created permission.

## Data Sources

Same as with permissions all components directly related to data sources are prefixed with `Datasource`. Since we use data sources across multiple components we manage the data source array in one place via a store [src/store/dataSources.ts](./src/stores/dataSources.ts). Note that data source images are managed within the same store. Data source images are available via a different endpoint than data sources themselves which is why we need an extra fetch request for each data source.

### DataSourceView

The view of the data source page. Contains the [DataSourceModal](#datasourcemodal), [MqttPasswordModal](#mqttpasswordmodal) as well as the [DataSourceList](#datasourcelist).

### DataSourceList

Component that manages a list of [DatasourceCard](#datasourcecard) components. Unlike permissions there is no filtering / sorting implemented here since the assumption is that users will add a far fewer amount of data sources then permissions.
Also handles all logic related to updating data sources like editing, deleting, reseting or toggling a data source.
This component fetches all data sources on mount and passes a permission object to the subcomponent so we do not have to refetch the data sources.

### DataSourceCard

Displays all information contained within a data source object + its image if it has one. On desktop it is displayed as a full card, however on mobile it is an expandable dropdown.

### Extending Data Sources

If you add a new data source type in the backend you might have to do various adjustments in the frontend for it to work properly. If you new data source uses props not needed by other data sources (like Modbus or Sinapsi) you need to do the following:

1. update the AiidaDataSource type in [types.d.ts](./src/types.d.ts) to handle the optional props.
2. update the form [DataSourceModal](./src/components/Modals/DataSourceModal.vue) to include new input field(s) for new data source props. These extra fields should be put into the `extra-column` and only be displayed if the selected dataSourceType matches your type. In addition form validation for new fields should also be addressed.
3. update [DataSourceCard](./src/components/DataSourceCard.vue) to include the new field(s)

## Modals

We use various modals to handle interactions with the REST API like adding permissions and data sources.
All of our modals extend our custom modal component [ModalDialog.vue](./src/components/ModalDialog.vue).

### ModalDialog

Basic custom modal component using the native [dialog](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/dialog) tag. With `dialog` we can make use of the semantic meaning of the tag as well as various native functionalites.

### AddPermissionModal

Modal which is displayed when a permission is added. Contains logic for parsing the base64 encoded AIIDA codes and also the [QRCodeScanner](./src/components/QrCodeScanner.vue).

### UpdatePermissionModal

Modal displayed when a permission has been successfully added or when a permission is continued.

### DataSourceModal

Modal displayed when a datasource is added or edited. Contains a form with all the needed information for adding data sources. Most data for this is fetched from the backend.

### MqttPasswordModal

Modal displayed after a MQTT data source was successfully added or when reseting the MQTT password. Allows users to quickly copy the password to setup the data source.

### ConfirmDialog

Modal displayed whenever the `confirm()` from the [confirm-dialog](./src/composables/confirm-dialog.ts) composeable is called. Implement as a custom version of the native `window.alert()` dialog field since that one can not be styled. We use this as double confirmation for deleting data sources or revoking permissions.

## Alerts

To show users proper feedback for their actions or to inform them when things go wrong we use toasts.
Via the [useToast](./src/composables/useToast.ts) composeable we manage a list of [AlertToast](./src/components/AlertToast.vue) components inside the [AlertToastList](./src/components/AlertToastList.vue). Toasts are typically only displayed for a set amount of time and can be created by calling either the general `notify()` function or the more specific functions `info()`, `warn()`, `danger()` and `success()`.

## Buttons

For all buttons and button links we use our custom [Button](./src/components/Button.vue) component. This component wraps its slot in the semantically correct tag (`<button>`or`<a>`) depending on the props provided and always has the correct button styling.

## Styling

We use basic CSS for all styling purposes. The Vue built in components `Transition` and `TransitionGroup` are used to help with styling list and view changes.
In addition to the scoped styles defined in the various components we also have 3 global CSS files:

- [reset](./src/assets/reset.css) - reset base browser styles
- [main](./src/assets/main.css) - contains general styles, global css variables and some global classes
- [typography](./src/assets/typography.css) - contains global classes for text

We do not have a strictly defined style guide for writing CSS, however we generally adhere to these rules:

- use classes instead of tag selectors
- CSS is written mobile first with media queries placed at the end of the style tag
- always use `<style scoped>`

## SVGs

All SVGs are located within [src/assets/icons](./src/assets/icons/).
Since we have a lot of different icons use the [vite-svg-loader](https://www.npmjs.com/package/vite-svg-loader/v/3.2.0) package so we can simply import SVGs as components inside .vue files. This package also uses SVGO to optimize and transform SVGs. To ensure proper SVG styling we remove the standard width and height dimension via the SVGO plugin "removeDimensions" set in [vite.config.ts](./vite.config.ts).

## I18n

We use the [vue-18n](https://vue-i18n.intlify.dev/) package to handle translations. This means that instead of writing text directly in component we use translation key alongside the `t()` function from `useI18n()` . All locale JSON files can be found in the [src/assets/locales](./src/assets/locales/) folder. We use english as the default and fallback locale.
If you want to add another locale, simply copy the `en.json`, rename it to the locale you want to add and translate the values.

## Keycloak

We use Keycloak for [various reasons](https://architecture.eddie.energy/aiida/1-running/keycloak.html). To integrate Keycloak into the frontend we use the official [keycloak-js](https://www.npmjs.com/package/keycloak-js) npm package. We defined a reusable `keycloak` object in [keycloak.ts](./src/keycloak.ts) which we use throughout the UI to handle authentication, login / logout and handling user data. Since we always need the `UUID` of the keycloak user to retrieve the permissions / data sources tied to that user from the backend, keycloak is initialized in [main.ts](./src/main.ts) and the UI will not work properly without logging in.

# Region Connector for France (Enedis)

This README will guide you through the process of configuring a region connector for one of the biggest permission
administrators in France.

## Prerequisites

- Register a user with Enedis [here](https://mon-compte.enedis.fr/auth/XUI/#login/&realm=/enedis). To register, try to
  log in with an unregistered email address and you will be prompted to register.
- Go to the [datahub](https://datahub-enedis.fr/) and log in with the user you registered.
- Navigate to "_Mon compte_" and then "_Nouvelle entité_" and enter into a contract for _DATA CONNECT_. Approval of this
  contract may take some time.
- Once the contact has been entered, navigate to "_Mon compte_" and then "_Mes applications_".
- Create an application in the __sandbox__ environment. This application will be used for getting an application in the
  __production__ environment approved.
- Once the application has been created, edit it and switch it to the __production__ environment. For this you will need
  to provide
  a valid callback URL. This URL needs to point to the region connector authorization-callback, for
  example `https://url-to-your-eddie-instance/region-connectors/fr-enedis/authorization-callback`. You will also need
  to provide a URL where _Enedis_ can check your running application in order to switch it to __production__. Point the
  URL to wherever you plan to host a __Connect with EDDIE__ button.
- Wait for the application to be approved. This may take some time.
- After it has been approved, update the client id and secret used
  to configure the region connector.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                                 | Description                                                                                                 |
|----------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `region-connector.fr.enedis.basepath`                                | Path to the data connect endpoints: https://ext.prod.api.enedis.fr for production.                          |
| `region-connector.fr.enedis.client.id`                               | Public key/id of the application you want to switch to production. Can be found under "_Mes applications_". |
| `region-connector.fr.enedis.client.secret`                           | Secret key of the application you want to switch to production. Can be found under "_Mes applications_".    |
| `region-connector.fr.enedis.threadpool.core.pool.size`               | How many threads are kept alive, also when idle (default 5)                                                 |
| `region-connector.fr.enedis.threadpool.max.pool.size`                | How many threads can exist simultaneously inside the thread pool (default 10)                               |
| `region-connector.fr.enedis.threadpool.queue.capacity`               | Amount of tasks to be queued before new threads are created (default 25)                                    |
| `region-connector.fr.enedis.tasks.permissions.per.task`              | How many permissions are handled per task (default 50)                                                      |
| `region-connector.fr.enedis.tasks.cron.future.data.permission.poll`  | When the future data is polled (default 10:30)                                                              |
| `region-connector.fr.enedis.tasks.cron.future.data.permission.clean` | When the clean up of future data permissions is happening (default 00:00)                                   |
### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.fr.enedis.basepath=https://ext.prod.api.enedis.fr
region-connector.fr.enedis.client.id=a5d5ce56-2bca-123d-1ccd-46a28f1ac132
region-connector.fr.enedis.client.secret=11d145d8-25a6-55c1-b6af-04ac332211b1
region-connector.fr.enedis.threadpool.core.pool.size=5
region-connector.fr.enedis.threadpool.max.pool.size=10
region-connector.fr.enedis.threadpool.queue.capacity=25
region-connector.fr.enedis.tasks.permissions.per.task=50
region-connector.fr.enedis.tasks.cron.future.data.permission.poll=0 30 10 * * ?
region-connector.fr.enedis.tasks.cron.future.data.permission.clean=0 0 0 * * ?
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_FR_ENEDIS_BASEPATH=https://ext.prod.api.enedis.fr
REGION_CONNECTOR_FR_ENEDIS_CLIENT_ID=a5d5ce56-2bca-123d-1ccd-46a28f1ac132
REGION_CONNECTOR_FR_ENEDIS_CLIENT_SECRET=11d145d8-25a6-55c1-b6af-04ac332211b1
REGION_CONNECTOR_THREAD_POOL_CORE_POOL_SIZE=5   
REGION_CONNECTOR_THREAD_POOL_MAX_POOL_SIZE=10   
REGION_CONNECTOR_THREAD_POOL_QUEUE_CAPACITY=25  
REGION_CONNECTOR_TASKS_PERMISSIONS_PER_TASK=50                           
REGION_CONNECTOR_TASKS_CRON_FUTURE_DATA_PERMISSION_POLL=0 30 10 * * ?
REGION_CONNECTOR_TASKS_CRON_FUTURE_DATA_PERMISSION_CLEAN=0 0 0 * * ? 
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
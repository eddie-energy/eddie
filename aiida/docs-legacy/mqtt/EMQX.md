# Documentation for the setup of EMQX MQTT broker

EMQX was decided to be used as the MQTT broker for AIIDA as it can handle the authentication and authorization more easily than NanoMQ.
NanoMQ used HTTP for authentication and authorization, which had limitations.

EMQX MQTT broker supports authentication and authorization using PostgreSQL as backend, which should use a dedicated EMQX user for the PostgreSQL database.
A dedicated user is created for the AIIDA, to authenticate at the EMQX MQTT broker.

## PostgreSQL: EMQX User Configuration

The PostgreSQL user `emqx` for the EMQX broker is created on the first startup of the EDDIE database.
If you would like to change the password for this user, you have to adapt the following value in AIIDA's `.env` file:

```
EMQX_DATABASE_PASSWORD=REPLACE_ME_WITH_SAFE_PASSWORD
```

## EMQX: AIIDA User Configuration

The EMQX user `aiida` for AIIDA is created on the first startup of the EMQX MQTT broker.
If you would like to change the password for this user, you have to adapt the following value in AIIDA's `.env` file:

```
MQTT_PASSWORD=REPLACE_ME_WITH_SAFE_PASSWORD
```
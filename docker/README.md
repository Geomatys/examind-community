# Examind Compose configurations

This folder contains multiple configurations to launch Examind in specific use-cases

## Standard configuration

This configuration deploys Examind in debug mode (for development environment).
A PostGIS database is deployed along it, to serve as administration database.
It can also be used to store Sensor related datasets (SensorThings, SOS).

To launch this configuration, run the default configuration file ([compose.yaml](./compose.yaml)) using your favorite compose engine.
Example: `docker compose up`

## Embedded database

Minimal configuration that runs a single Container (which runs a single JVM) to run both Examind and its administration database.
The database is opened as an embedded database file using HsqlDB.
In this mode, it is not possible to use the database to store sensor datasets, and if your need to use Sensor services (SensorThings, SOS), you will have to run and connect your own database separately.

To launch, use the [compose-embedded-db.yaml](./compose-embedded-db.yaml) file.
Example: `docker compose -f compose-embedded-db.yaml up`

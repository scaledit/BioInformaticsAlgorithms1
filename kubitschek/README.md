# [![nToggle](http://static1.squarespace.com/static/54aff901e4b09d0a90426749/t/54aff95fe4b0fbcca432e54d/1423161066764/?format=1500w)](http://www.ntoggle.com/) kubitschek

* [![Circle CI](https://circleci.com/gh/nToggle/kubitschek/tree/master.svg?style=svg&circle-token=cb898e9c4f0a24af2d0a864567cb8afe0e2d74d0)](https://circleci.com/gh/nToggle/kubitschek/tree/master) `master`
* [![Circle CI](https://circleci.com/gh/nToggle/kubitschek.svg?style=svg&circle-token=cb898e9c4f0a24af2d0a864567cb8afe0e2d74d0)](https://circleci.com/gh/nToggle/kubitschek) `develop`

# Project for public configuration API

The configuration file is at
```
src/main/resources/application.conf
```

The application requires a running instance of Postgres running on the default port, or else
set environment variable JK_ENV to use one of the memory persistence configurations (see below).

There is DDL is in 'src/main/resources/ddl' folder.
You must manually create the database using parameters from the configuration file

```
src/main/resources/application.conf
```

Assuming you added your user login to the Postgres instance already:

```
sudo -u postgres psql

create user ##user-from-conf-file## password '##pw-from-conf-file##';
create database ##dbname-from-conf-file## owner ##user-from-conf-file##;
```

From the src/ddl folder:

```
psql -U ##user-from-conf-file## -h localhost -d ##dbname-from-conf-file## -f rules.sql
```

NOTE: the first time you run this, you will get a warning about dropping the table because it doesn't exist.
For example:

    psql:rules.sql:1: NOTICE:  table "demand_partners" does not exist, skipping


Verify this worked:

```
sudo -u postgres psql

\c ##dbname-from-conf-file##

\dt
```

# API Documentation

The JSON Swagger API documentation can be found at: `/api/v1/docs/api-client.json`

There is also a person-friendly UI at: `/api/v1/docs/`

# API Client Generator

The folder
```
swagggerClientGenerator
```

contains tools to build an Angular JS client file from the Swagger api.json file in this project.

# Environment Settings

The environment variable JK_ENV controls the database configuration and other settings.
The server fails to start if this variable is unset or an unknown value.
The following error will be logged:

[error] Exception in thread "main" com.typesafe.config.ConfigException$BadValue: Invalid value at 'envType': Unknown or unset 'JK_ENV'

Set JK_ENV to one of
##development
Use locally hosted postgres on the default port.
##memory
No database, uses the MemPersistence with a couple of default DP and SP objects.
#memory-empty
No database, uses empty MemPersistence.
##integration
Use the instance of Postgres hosted on our integration environment.


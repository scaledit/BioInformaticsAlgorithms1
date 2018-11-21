#!/bin/bash

export PGUSER=postgres
sqls=( "/ddl/rules.sql" )

psql <<-EOS
  CREATE DATABASE kubitschek;
  CREATE USER kubitschek;
  GRANT ALL PRIVILEGES ON DATABASE kubitschek to kubitschek;
  CREATE USER datadog with password '7Dfn0HLFylMKfVDf5fN2dSBE';
  grant SELECT ON pg_stat_database to datadog;
EOS

for sql in "${sqls[@]}"
do
  psql --dbname kubitschek -U kubitschek < "$sql"
done

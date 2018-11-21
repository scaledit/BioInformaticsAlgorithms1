---
--- DO NOT MODIFY THIS TABLE WITHOUT CONSULTING DEVOPS AND HELIX TEAM.
--- There are command line tools that depend on this table.
---
DROP TABLE IF EXISTS user_lists CASCADE;
CREATE TABLE user_lists(
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  dp_id VARCHAR(36) NOT NULL REFERENCES demand_partners(id),
  created TIMESTAMP NOT NULL,
  modified TIMESTAMP NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (name, dp_id));

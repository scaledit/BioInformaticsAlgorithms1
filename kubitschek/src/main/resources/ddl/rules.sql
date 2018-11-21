
DROP TABLE IF EXISTS demand_partners CASCADE;
CREATE TABLE demand_partners (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(64) UNIQUE NOT NULL);

DROP TABLE IF EXISTS supply_partners CASCADE;
CREATE TABLE supply_partners (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(64) UNIQUE NOT NULL);
-- Defines a relationship between DemandPartner, SupplyPartner
DROP TABLE IF EXISTS router_configurations CASCADE;
CREATE TABLE router_configurations(
  dp_id VARCHAR(36) NOT NULL REFERENCES demand_partners(id),
  sp_id VARCHAR(36) NOT NULL,
  enabled  BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (sp_id) REFERENCES supply_partners (id),
  PRIMARY KEY(dp_id, sp_id));

-- Defines router configuration endpoints associated with a DemandPartner, SupplyPartner
DROP TABLE IF EXISTS router_configuration_endpoints CASCADE;
CREATE TABLE router_configuration_endpoints(
  dp_id VARCHAR(36) NOT NULL,
  sp_id VARCHAR(36) NOT NULL,
  host VARCHAR(255) NOT NULL,
  port INT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (dp_id, sp_id) REFERENCES router_configurations (dp_id, sp_id),
  PRIMARY KEY(dp_id, sp_id, host, port));

-- Defines versions of a router configuration
DROP TABLE IF EXISTS versions CASCADE;
CREATE TABLE versions(
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  dp_id VARCHAR(36) NOT NULL,
  sp_id VARCHAR(36) NOT NULL,
  max_qps INT NOT NULL,
  created TIMESTAMP NOT NULL,
  modified TIMESTAMP NOT NULL,
  published TIMESTAMP,
  FOREIGN KEY (dp_id, sp_id) REFERENCES router_configurations (dp_id, sp_id));

-- Business logic/API should enforce that Rules are immutable
-- as they could be shared between versions
DROP TABLE IF EXISTS rules CASCADE;
CREATE TABLE rules(
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  created TIMESTAMP NOT NULL,
  traffic_type VARCHAR(36) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE);

DROP TABLE IF EXISTS versions_rules CASCADE;
CREATE TABLE versions_rules(
  rule_id VARCHAR(36) NOT NULL REFERENCES rules(id),
  version_id VARCHAR(36) NOT NULL REFERENCES versions(id),
  desired_qps INT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (rule_id, version_id));

-- Reference table for Rule condition actions
-- Enums not supported by some DB
DROP TABLE IF EXISTS rule_condition_action_type CASCADE;
CREATE TABLE rule_condition_action_type (
  id INT NOT NULL PRIMARY KEY,
  action_type VARCHAR(10) NOT NULL UNIQUE);

INSERT INTO rule_condition_action_type (id, action_type)
VALUES (0, 'block'),
       (1, 'allow');

DROP TABLE IF EXISTS rule_conditions CASCADE;
CREATE TABLE rule_conditions(
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  rule_id VARCHAR(36) NOT NULL REFERENCES rules(id),
  attribute_type VARCHAR(36) NOT NULL, -- this is handset, ad size, etc.
  default_action INT NOT NULL REFERENCES rule_condition_action_type(id),
  undefined_action INT NOT NULL REFERENCES rule_condition_action_type(id),
  UNIQUE (rule_id, attribute_type));

DROP TABLE IF EXISTS rule_condition_exceptions CASCADE;
create table rule_condition_exceptions(
  rule_condition_id VARCHAR(36) NOT NULL REFERENCES rule_conditions(id),
  feature_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (rule_condition_id, feature_id));

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

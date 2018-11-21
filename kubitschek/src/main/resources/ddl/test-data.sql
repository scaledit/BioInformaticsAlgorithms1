
-- insert some standard data in the database

-- These are the same id's as MemPersistence

INSERT INTO demand_partners (id, name) values ('0d1a63f2-3f45-4fb1-9237-0b7cfed3d11f', 'TEST-DP-1');
INSERT INTO demand_partners (id, name) values ('3f214842-6b16-483f-b49f-d152e01c6e33', 'TEST-DP-2');
INSERT INTO supply_partners (id, name) values ('9a726b7f-e36a-441f-b83a-2581c0edfcd3', 'TEST-SP-1');
INSERT INTO supply_partners (id, name) values ('dc8333de-baa7-4f6b-babd-f0b1a87abbc5', 'TEST-SP-2');
INSERT INTO supply_partners (id, name) values ('b72331e0-eee2-493f-92e8-6a0500294d70', 'TEST-SP-3');

-- DP1,SP1
INSERT INTO router_configurations (dp_id, sp_id) values ('0d1a63f2-3f45-4fb1-9237-0b7cfed3d11f', '9a726b7f-e36a-441f-b83a-2581c0edfcd3');
INSERT INTO router_configuration_endpoints (dp_id, sp_id, host, port) values ('0d1a63f2-3f45-4fb1-9237-0b7cfed3d11f', '9a726b7f-e36a-441f-b83a-2581c0edfcd3', 'test-host-1', '9011');

-- DP2,SP2
INSERT INTO router_configurations (dp_id, sp_id) values ('3f214842-6b16-483f-b49f-d152e01c6e33', 'dc8333de-baa7-4f6b-babd-f0b1a87abbc5');
INSERT INTO router_configuration_endpoints (dp_id, sp_id, host, port) values ('3f214842-6b16-483f-b49f-d152e01c6e33', 'dc8333de-baa7-4f6b-babd-f0b1a87abbc5', 'test-host-2', '9012');

-- DP2, SP3
INSERT INTO router_configurations (dp_id, sp_id) values ('3f214842-6b16-483f-b49f-d152e01c6e33', 'b72331e0-eee2-493f-92e8-6a0500294d70');
INSERT INTO router_configuration_endpoints (dp_id, sp_id, host, port) values ('3f214842-6b16-483f-b49f-d152e01c6e33', 'b72331e0-eee2-493f-92e8-6a0500294d70', 'test-host-3', '9013');

INSERT INTO versions (id, dp_id, sp_id, max_qps, created, modified) values ('2e83b2ea-0e9e-4d4d-a330-a1d0b9065270' ,'0d1a63f2-3f45-4fb1-9237-0b7cfed3d11f', '9a726b7f-e36a-441f-b83a-2581c0edfcd3', '100021', '1970-01-01 00:00:33.333', '1970-01-01 00:00:33.333');
INSERT INTO versions (id, dp_id, sp_id, max_qps, created, modified) values ('c1c43a16-9829-4863-a4e8-2c6ae99e5a6e' ,'3f214842-6b16-483f-b49f-d152e01c6e33', 'dc8333de-baa7-4f6b-babd-f0b1a87abbc5', '100022', '1970-02-02 00:00:33.333', '1970-01-01 00:00:33.333');
INSERT INTO versions (id, dp_id, sp_id, max_qps, created, modified) values ('76637df2-db5b-413c-95e0-cb111d1f2b88' ,'3f214842-6b16-483f-b49f-d152e01c6e33', 'b72331e0-eee2-493f-92e8-6a0500294d70', '100023', '1970-03-03 00:00:33.333', '1970-01-01 00:00:33.333');

--
-- These are arbitrary id's
--

INSERT INTO demand_partners (id, name) values ('b20195b1-7811-4e4d-bdc3-f81943654564', 'TEST-DP-5');
INSERT INTO supply_partners (id, name) values ('6289c608-5ed9-42fa-ba8b-4d4499b9e033', 'TEST-SP-5');
INSERT INTO supply_partners (id, name) values ('d7f40e89-53b7-4cd9-a0a5-eda715d6d485', 'TEST-SP-6');

-- dp5, sp5
INSERT INTO router_configurations (dp_id, sp_id) values ('b20195b1-7811-4e4d-bdc3-f81943654564', '6289c608-5ed9-42fa-ba8b-4d4499b9e033');
INSERT INTO router_configuration_endpoints (dp_id, sp_id, host, port) values ('b20195b1-7811-4e4d-bdc3-f81943654564', '6289c608-5ed9-42fa-ba8b-4d4499b9e033', 'test-host-5', '9015');

INSERT INTO versions (id, dp_id, sp_id, max_qps, created, modified) values ('06ea485d-a566-4bb9-ae1c-24aa33abfd6f', 'b20195b1-7811-4e4d-bdc3-f81943654564', '6289c608-5ed9-42fa-ba8b-4d4499b9e033', '100025', '1970-05-05 00:00:33.333', '1970-05-05 00:00:33.333');

--- Arbitrary UUID for id, but dp_id must exist.
INSERT INTO user_lists (id,name,dp_id,created,modified,enabled) values ('515f5f75-3a46-4f01-8d49-53ccf28457e7', 'auto-something', '0d1a63f2-3f45-4fb1-9237-0b7cfed3d11f', '1970-01-01 00:00:33.333', '1970-01-01 00:00:33.333', true);
INSERT INTO user_lists (id,name,dp_id,created,modified,enabled) values ('bcbfa39c-7879-498d-ae28-1a1c68b6aa35', 'auto-something', '3f214842-6b16-483f-b49f-d152e01c6e33', '1970-01-01 00:00:33.333', '1970-01-01 00:00:33.333', true);
INSERT INTO user_lists (id,name,dp_id,created,modified,enabled) values ('c535561b-b999-403f-acd3-f319db6e9876', 'auto-something', 'b20195b1-7811-4e4d-bdc3-f81943654564', '1970-01-01 00:00:33.333', '1970-01-01 00:00:33.333', true);

-- testDeleteMessage
INSERT INTO message (message_id, message_box, message_type, cpa_id, service, action, conv_id, status, time_stamp) VALUES ('del-out@test', 'outbox', 'Order', 'cpaX', 'svc', 'act', 'c1', 'DL', '2026-01-01 00:00:00');
INSERT INTO repository (message_id, message_box, content, time_stamp) VALUES ('del-out@test', 'outbox', NULL, '2026-01-01 00:00:00');
INSERT INTO outbox (message_id, retried) VALUES ('del-out@test', 0);
INSERT INTO message (message_id, message_box, message_type, cpa_id, service, action, conv_id, status, time_stamp) VALUES ('del-in@test', 'inbox', 'Order', 'cpaX', 'svc', 'act', 'c1', 'PS', '2026-01-01 00:00:00');
INSERT INTO repository (message_id, message_box, content, time_stamp) VALUES ('del-in@test', 'inbox', NULL, '2026-01-01 00:00:00');
INSERT INTO inbox (message_id, order_no) VALUES ('del-in@test', 1);
INSERT INTO message (message_id, message_box, message_type, cpa_id, service, action, conv_id, status, time_stamp) VALUES ('keep@test', 'outbox', 'Order', 'cpaX', 'svc', 'act', 'c1', 'DL', '2026-01-01 00:00:00');
INSERT INTO repository (message_id, message_box, content, time_stamp) VALUES ('keep@test', 'outbox', NULL, '2026-01-01 00:00:00');
INSERT INTO outbox (message_id, retried) VALUES ('keep@test', 0);

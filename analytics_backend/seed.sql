INSERT INTO crashes (module_name, error_message) VALUES
  ('NewExpenseScreen', 'Test error'),
  ('SyncService', 'Network timeout'),
  ('AutoCategorizationService', 'Pinecone unreachable');

INSERT INTO days_since_expense (user_id, days, is_inactive) VALUES
  (1, 5, true),
  (2, 1, false),
  (3, 10, true);

INSERT INTO ocr_edit_rate (user_id, fields_populated, fields_edited, edit_rate) VALUES
  (1, 5, 3, 0.6),
  (1, 4, 1, 0.25),
  (2, 5, 5, 1.0),
  (2, 3, 0, 0.0);

INSERT INTO active_hours (user_id, hour, session) VALUES
  (1, 9,  'morning'),
  (1, 14, 'afternoon'),
  (2, 21, 'evening'),
  (3, 9,  'morning'),
  (3, 8,  'morning');

INSERT INTO small_recurring_expenses (user_id, count, total_amount) VALUES
  (1, 4, 180000),
  (2, 2, 75000);

INSERT INTO registration_methods (user_id, manual_count, ocr_count, google_pay_count, least_used_method) VALUES
  (1, 10, 3, 0, 'google_pay'),
  (2,  5, 8, 1, 'google_pay');

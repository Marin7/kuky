-- Payment instructions are no longer shown or edited in-app for the placement
-- test's full evaluation; the bank-transfer conversation happens entirely
-- offline between the teacher and the student.

ALTER TABLE placement_config DROP COLUMN payment_instructions;

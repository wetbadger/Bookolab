BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (10001, 'The');
INSERT INTO page (id, first_word_id, last_word_id) VALUES (1, 10001, 10001);

COMMIT;

BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (10002, 'quick');
UPDATE word SET next_word_id = 10002 WHERE id = 10001;
UPDATE page SET last_word_id = 10002 WHERE id = 1;

COMMIT;

BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (10003, 'brown');
UPDATE word SET next_word_id = 10003 WHERE id = 10002;
UPDATE page SET last_word_id = 10003 WHERE id = 1;

COMMIT;

BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (10004, 'fox');
UPDATE word SET next_word_id = 10004 WHERE id = 10003;
UPDATE page SET last_word_id = 10004 WHERE id = 1;

COMMIT;

-- Page 2

BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (20001, 'jumps');
UPDATE word SET next_word_id = 20001 WHERE id = 10004;
INSERT INTO page (id, first_word_id, last_word_id) VALUES (2, 20001, 20001);

COMMIT;

BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (20002, 'over');
UPDATE word SET next_word_id = 20002 WHERE id = 20001;
UPDATE page SET last_word_id = 20002 WHERE id = 2;

COMMIT;

BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (20003, 'the');
UPDATE word SET next_word_id = 20003 WHERE id = 20002;
UPDATE page SET last_word_id = 20003 WHERE id = 2;

COMMIT;

BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (20004, 'lazy');
UPDATE word SET next_word_id = 20004 WHERE id = 20003;
UPDATE page SET last_word_id = 20004 WHERE id = 2;

COMMIT;

-- Page 3

BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (30001, 'dog.');
UPDATE word SET next_word_id = 30001 WHERE id = 20004;
INSERT INTO page (id, first_word_id, last_word_id) VALUES (3, 30001, 30001);

COMMIT;
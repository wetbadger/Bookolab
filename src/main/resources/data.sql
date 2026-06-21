-- Rename this to data.sql if you are using a fresh database

-- Add this to the absolute top of data.sql
TRUNCATE TABLE page, word RESTART IDENTITY CASCADE;

-- Page 1
INSERT INTO word (id, content, local_id) VALUES (10001, 'The', 'abcdefg');
INSERT INTO page (id, first_word_id, last_word_id) VALUES (1, 10001, 10001);

INSERT INTO word (id, content, local_id) VALUES (10002, 'quick', 'hijklmnop');
UPDATE word SET next_word_id = 10002 WHERE id = 10001;
UPDATE page SET last_word_id = 10002 WHERE id = 1;

INSERT INTO word (id, content, local_id) VALUES (10003, 'brown', 'qrstuv');
UPDATE word SET next_word_id = 10003 WHERE id = 10002;
UPDATE page SET last_word_id = 10003 WHERE id = 1;

INSERT INTO word (id, content, local_id) VALUES (10004, 'fox', 'wxyz');
UPDATE word SET next_word_id = 10004 WHERE id = 10003;
UPDATE page SET last_word_id = 10004 WHERE id = 1;

-- Page 2
INSERT INTO word (id, content, local_id) VALUES (20001, 'jumps', 'abc20001');
UPDATE word SET next_word_id = 20001 WHERE id = 10004;
INSERT INTO page (id, first_word_id, last_word_id) VALUES (2, 20001, 20001);

INSERT INTO word (id, content, local_id) VALUES (20002, 'over', 'def20002');
UPDATE word SET next_word_id = 20002 WHERE id = 20001;
UPDATE page SET last_word_id = 20002 WHERE id = 2;

INSERT INTO word (id, content, local_id) VALUES (20003, 'the', 'ghi20003');
UPDATE word SET next_word_id = 20003 WHERE id = 20002;
UPDATE page SET last_word_id = 20003 WHERE id = 2;

INSERT INTO word (id, content, local_id) VALUES (20004, 'lazy', 'jkl20004');
UPDATE word SET next_word_id = 20004 WHERE id = 20003;
UPDATE page SET last_word_id = 20004 WHERE id = 2;

-- Page 3
INSERT INTO word (id, content, local_id) VALUES (30001, 'dog.', 'mno30001');
UPDATE word SET next_word_id = 30001 WHERE id = 20004;
INSERT INTO page (id, first_word_id, last_word_id) VALUES (3, 30001, 30001);

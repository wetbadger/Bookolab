BEGIN TRANSACTION;

INSERT INTO word (id, content) VALUES (10001, 'The');
INSERT INTO page (id, first_word_id, last_word_id) VALUES (1, 10001, 10001);

COMMIT;
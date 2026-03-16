-- Testovacie preferencie pre Marec 2026
-- Víkendy: 1(Ne), 7(So), 8(Ne), 14(So), 15(Ne), 21(So), 22(Ne), 28(So), 29(Ne)

-- TESTOVANIE: všetci nemôžu 1. marca (nedeľa) -> forced priradenie
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(1, '2026-03-01', 'CANNOT_ON_CALL', 'test - nemôžem'),
(2, '2026-03-01', 'CANNOT_ON_CALL', 'test - nemôžem'),
(3, '2026-03-01', 'CANNOT_ON_CALL', 'test - nemôžem'),
(4, '2026-03-01', 'CANNOT_ON_CALL', 'test - nemôžem'),
(6, '2026-03-01', 'CANNOT_ON_CALL', 'test - nemôžem');

-- Kika Kováčová (id=1) - chce slúžiť v týždni, víkend 14-15 nemôže (rodinná oslava)
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(1, '2026-03-05', 'WANT_ON_CALL', 'vyhovuje mi štvrtok'),
(1, '2026-03-07', 'WANT_ON_CALL', 'chcela by som víkend'),
(1, '2026-03-12', 'WANT_ON_CALL', NULL),
(1, '2026-03-14', 'CANNOT_ON_CALL', 'rodinná oslava'),
(1, '2026-03-15', 'CANNOT_ON_CALL', 'rodinná oslava'),
(1, '2026-03-22', 'WANT_ON_CALL', 'tento víkend môžem');

-- Lenka Horváthová (id=2) - víkend 7-8 nemôže, wants_extra_shift=TRUE v doctor tabuľke
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(2, '2026-03-07', 'CANNOT_ON_CALL', 'svadba kamarátky'),
(2, '2026-03-08', 'CANNOT_ON_CALL', 'svadba kamarátky'),
(2, '2026-03-21', 'WANT_ON_CALL', NULL),
(2, '2026-03-28', 'WANT_ON_CALL', NULL);

-- Júlia Novotná (id=3) - dovolenka 21-29, chce slúžiť víkend 7
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(3, '2026-03-07', 'WANT_ON_CALL', 'môžem cez víkend'),
(3, '2026-03-08', 'WANT_ON_CALL', NULL),
(3, '2026-03-21', 'CANNOT_ON_CALL', 'dovolenka'),
(3, '2026-03-22', 'CANNOT_ON_CALL', 'dovolenka'),
(3, '2026-03-23', 'CANNOT_ON_CALL', 'dovolenka'),
(3, '2026-03-24', 'CANNOT_ON_CALL', 'dovolenka'),
(3, '2026-03-25', 'CANNOT_ON_CALL', 'dovolenka'),
(3, '2026-03-26', 'CANNOT_ON_CALL', 'dovolenka'),
(3, '2026-03-27', 'CANNOT_ON_CALL', 'dovolenka'),
(3, '2026-03-28', 'CANNOT_ON_CALL', 'dovolenka'),
(3, '2026-03-29', 'CANNOT_ON_CALL', 'dovolenka');

-- Marko Baláž (id=4) - preferuje víkendy, 3-4 nemôže (vyšetrenie)
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(4, '2026-03-03', 'CANNOT_ON_CALL', 'lekárske vyšetrenie'),
(4, '2026-03-04', 'CANNOT_ON_CALL', 'lekárske vyšetrenie'),
(4, '2026-03-14', 'WANT_ON_CALL', 'preferujem víkendy'),
(4, '2026-03-15', 'WANT_ON_CALL', NULL),
(4, '2026-03-18', 'WANT_ON_CALL', 'chcel by som stredu'),
(4, '2026-03-29', 'WANT_ON_CALL', NULL);

-- Martin Tóth (id=5) - 1. marca nemôže, chce stredy
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(5, '2026-03-01', 'CANNOT_ON_CALL', 'sťahovanie'),
(5, '2026-03-04', 'WANT_ON_CALL', 'stredy mi vyhovujú'),
(5, '2026-03-11', 'WANT_ON_CALL', 'stredy mi vyhovujú'),
(5, '2026-03-18', 'WANT_ON_CALL', NULL),
(5, '2026-03-28', 'WANT_ON_CALL', 'môžem víkend');

-- Tomáš Varga (id=6) - wants_extra_shift=TRUE, 29. nemôže
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(6, '2026-03-09', 'WANT_ON_CALL', NULL),
(6, '2026-03-16', 'WANT_ON_CALL', NULL),
(6, '2026-03-29', 'CANNOT_ON_CALL', 'narodeniny dcéry');

-- Testovacie preferencie pre Apríl 2026

-- Kika - dovolenka prvý týždeň
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(1, '2026-04-01', 'CANNOT_ON_CALL', 'dovolenka'),
(1, '2026-04-02', 'CANNOT_ON_CALL', 'dovolenka'),
(1, '2026-04-03', 'CANNOT_ON_CALL', 'dovolenka'),
(1, '2026-04-04', 'CANNOT_ON_CALL', 'dovolenka'),
(1, '2026-04-05', 'CANNOT_ON_CALL', 'dovolenka'),
(1, '2026-04-18', 'WANT_ON_CALL', NULL);

-- Lenka - chce víkend v apríli
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(2, '2026-04-11', 'WANT_ON_CALL', NULL);

-- Marko - Veľká noc (5-6.4.) nemôže
INSERT INTO shift_preference (doctor_id, date, preference_type, note) VALUES
(4, '2026-04-05', 'CANNOT_ON_CALL', 'Veľká noc'),
(4, '2026-04-06', 'CANNOT_ON_CALL', 'Veľká noc'),
(4, '2026-04-25', 'WANT_ON_CALL', NULL);

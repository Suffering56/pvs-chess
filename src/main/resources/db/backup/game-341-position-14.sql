DELETE FROM public.history WHERE game_id = 341;
DELETE FROM public.game_features WHERE game_id = 341;
DELETE FROM public.game WHERE id = 341;

INSERT INTO public.game (id, mode, position) VALUES (341, 'AI', 14);

INSERT INTO public.game_features (id, is_under_check, last_visit_date, long_castling_available, pawn_long_move_column_index, session_id, short_castling_available, side, game_id) VALUES (681, false, '2018-10-17 17:20:25.588000', false, null, '2E105CA4A3786ED662870D01B5808798', false, 'WHITE', 341);
INSERT INTO public.game_features (id, is_under_check, last_visit_date, long_castling_available, pawn_long_move_column_index, session_id, short_castling_available, side, game_id) VALUES (682, false, '2018-10-17 17:20:29.808000', true, null, null, true, 'BLACK', 341);

INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101436, 1, 341, 6, 14, 0);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101437, 2, 341, 4, 14, 0);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101438, 6, 341, 2, 14, 0);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101439, 7, 341, 4, 14, 0);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101440, 0, 341, 1, 14, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101441, 1, 341, 1, 14, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101442, 2, 341, 1, 14, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101443, 5, 341, 1, 14, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101444, 6, 341, 1, 14, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101445, 7, 341, 1, 14, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101446, 2, 341, 5, 14, 2);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101447, 4, 341, 1, 14, 2);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101448, 3, 341, 1, 14, 3);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101449, 5, 341, 3, 14, 3);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101450, 1, 341, 3, 14, 4);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101451, 3, 341, 7, 14, 4);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101452, 7, 341, 7, 14, 4);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101453, 1, 341, 11, 14, 5);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101454, 0, 341, 7, 14, 6);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101455, 1, 341, 7, 14, 6);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101456, 2, 341, 7, 14, 6);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101457, 4, 341, 7, 14, 6);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101458, 5, 341, 7, 14, 6);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101459, 6, 341, 7, 14, 6);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101460, 0, 341, 10, 14, 7);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101461, 1, 341, 8, 14, 7);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101462, 2, 341, 9, 14, 7);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101463, 3, 341, 12, 14, 7);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101464, 5, 341, 9, 14, 7);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (101465, 7, 341, 10, 14, 7);
DELETE FROM public.history WHERE game_id = 160;
DELETE FROM public.game_features WHERE game_id = 160;
DELETE FROM public.game WHERE id = 160;

INSERT INTO public.game (id, mode, position) VALUES (160, 'AI', 28);

INSERT INTO public.game_features (id, is_under_check, last_visit_date, long_castling_available, pawn_long_move_column_index, session_id, short_castling_available, side, game_id) VALUES (320, false, '2018-10-05 15:41:09.443000', false, null, '7756457BC1653316B99E043BD50E5CAC', true, 'WHITE', 160);
INSERT INTO public.game_features (id, is_under_check, last_visit_date, long_castling_available, pawn_long_move_column_index, session_id, short_castling_available, side, game_id) VALUES (319, false, '2018-10-05 15:41:09.783000', false, null, null, false, 'BLACK', 160);

INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (0, 160, 4, 28, 0);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (1, 160, 2, 28, 0);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (3, 160, 6, 28, 0);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (6, 160, 4, 28, 0);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (0, 160, 1, 28, 1);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (1, 160, 1, 28, 1);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (6, 160, 1, 28, 1);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (7, 160, 1, 28, 1);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (2, 160, 1, 28, 2);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (4, 160, 3, 28, 2);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (5, 160, 1, 28, 2);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (1, 160, 9, 28, 3);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (4, 160, 1, 28, 3);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (4, 160, 7, 28, 4);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (5, 160, 7, 28, 4);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (7, 160, 5, 28, 4);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (0, 160, 7, 28, 5);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (2, 160, 12, 28, 5);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (3, 160, 7, 28, 5);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (4, 160, 9, 28, 5);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (7, 160, 7, 28, 5);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (1, 160, 7, 28, 6);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (2, 160, 7, 28, 6);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (5, 160, 3, 28, 6);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (6, 160, 7, 28, 6);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (0, 160, 10, 28, 7);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (1, 160, 8, 28, 7);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (4, 160, 11, 28, 7);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (6, 160, 8, 28, 7);
INSERT INTO public.history (column_index, game_id, piece_id, position, row_index) VALUES (7, 160, 10, 28, 7);


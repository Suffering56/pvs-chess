--liquibase formatted sql

--changeset Magic:1
INSERT INTO public.piece (id, side, type) VALUES (1, 'white', 'pawn');
INSERT INTO public.piece (id, side, type) VALUES (2, 'white', 'knight');
INSERT INTO public.piece (id, side, type) VALUES (3, 'white', 'bishop');
INSERT INTO public.piece (id, side, type) VALUES (4, 'white', 'rook');
INSERT INTO public.piece (id, side, type) VALUES (5, 'white', 'queen');
INSERT INTO public.piece (id, side, type) VALUES (6, 'white', 'king');
INSERT INTO public.piece (id, side, type) VALUES (7, 'black', 'pawn');
INSERT INTO public.piece (id, side, type) VALUES (8, 'black', 'knight');
INSERT INTO public.piece (id, side, type) VALUES (9, 'black', 'bishop');
INSERT INTO public.piece (id, side, type) VALUES (10, 'black', 'rook');
INSERT INTO public.piece (id, side, type) VALUES (11, 'black', 'queen');
INSERT INTO public.piece (id, side, type) VALUES (12, 'black', 'king');
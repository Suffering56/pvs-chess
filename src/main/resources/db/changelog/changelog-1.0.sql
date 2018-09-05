--liquibase formatted sql

--changeset Magic:1
create table if not exists piece (
	id bigint not null constraint piece_pkey primary key,
	side varchar(255) not null,
	type varchar(255) not null
);

INSERT INTO public.piece (id, side, type) VALUES (1, 'WHITE', 'PAWN');
INSERT INTO public.piece (id, side, type) VALUES (2, 'WHITE', 'KNIGHT');
INSERT INTO public.piece (id, side, type) VALUES (3, 'WHITE', 'BISHOP');
INSERT INTO public.piece (id, side, type) VALUES (4, 'WHITE', 'ROOK');
INSERT INTO public.piece (id, side, type) VALUES (5, 'WHITE', 'QUEEN');
INSERT INTO public.piece (id, side, type) VALUES (6, 'WHITE', 'KING');
INSERT INTO public.piece (id, side, type) VALUES (7, 'BLACK', 'PAWN');
INSERT INTO public.piece (id, side, type) VALUES (8, 'BLACK', 'KNIGHT');
INSERT INTO public.piece (id, side, type) VALUES (9, 'BLACK', 'BISHOP');
INSERT INTO public.piece (id, side, type) VALUES (10, 'BLACK', 'ROOK');
INSERT INTO public.piece (id, side, type) VALUES (11, 'BLACK', 'QUEEN');
INSERT INTO public.piece (id, side, type) VALUES (12, 'BLACK', 'KING');


--changeset Magic:2
create table game (
	id bigint not null constraint game_pkey primary key,
	mode varchar(255),
	position integer default 0 not null
);
create sequence game_id_seq;


create table game_features (
	id bigint not null constraint game_features_pkey primary key,
	is_under_check boolean default false not null,
	last_visit_date timestamp,
	long_castling_available boolean default true not null,
	pawn_long_move_column_index integer,
	session_id varchar(255),
	short_castling_available boolean default true not null,
	side varchar(255) not null,
	game_id bigint not null constraint fk63xltct60scimpm06k8bhbe4a references game
);
create sequence game_features_id_seq;


create table history (
	id bigint not null constraint history_pkey primary key,
	column_index integer not null,
	game_id bigint not null,
	piece_id integer not null constraint fkkibbi3mdi0bqj6uvyv0apiquf references piece,
	position integer not null,
	row_index integer not null
);
create sequence history_id_seq;




-- ============================================================
-- V1__init_schema.sql
-- Bingo MVP: initial schema (PostgreSQL 13+)
-- ============================================================
-- Notes:
--  * Enums are modeled as varchar + CHECK (simpler with JDBC than
--    native Postgres enums, which need explicit casts).
--  * UUIDs default to gen_random_uuid() (built in since PostgreSQL 13),
--    but the application can also generate and send its own ids.
-- ============================================================


-- ------------------------------------------------------------
-- app_user: registered account (creates and owns games)
-- ------------------------------------------------------------
CREATE TABLE app_user (
                          id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
                          name          varchar(100) NOT NULL,
                          email         varchar(255) NOT NULL,
                          password_hash varchar(255) NOT NULL,
                          created_at    timestamptz  NOT NULL DEFAULT now(),

                          CONSTRAINT uq_app_user_email UNIQUE (email)
);


-- ------------------------------------------------------------
-- game: a bingo match (75 balls, full card to win)
-- winner_card_id FK is added after the card table exists.
-- ------------------------------------------------------------
CREATE TABLE game (
                      id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
                      name            varchar(100) NOT NULL,
                      owner_id        uuid         NOT NULL,
                      join_code       varchar(20)  NOT NULL,
                      status          varchar(20)  NOT NULL DEFAULT 'WAITING',
                      mode            varchar(20)  NOT NULL,
                      winner_card_id  uuid,
                      created_at      timestamptz  NOT NULL DEFAULT now(),
                      started_at      timestamptz,
                      finished_at     timestamptz,
                      bingo_at        timestamptz,

                      CONSTRAINT uq_game_join_code UNIQUE (join_code),
                      CONSTRAINT fk_game_owner FOREIGN KEY (owner_id) REFERENCES app_user (id),
                      CONSTRAINT chk_game_status CHECK (status IN ('WAITING', 'RUNNING', 'FINISHED', 'CANCELLED')),
                      CONSTRAINT chk_game_mode   CHECK (mode IN ('AUTOMATIC', 'MANUAL'))
);

CREATE INDEX idx_game_owner_id ON game (owner_id);

-- The draw mode is chosen at creation and can never change.
CREATE FUNCTION prevent_game_mode_change() RETURNS trigger AS $$
BEGIN
    IF NEW.mode IS DISTINCT FROM OLD.mode THEN
        RAISE EXCEPTION 'game.mode is immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_game_mode_immutable
    BEFORE UPDATE OF mode ON game
    FOR EACH ROW
EXECUTE FUNCTION prevent_game_mode_change();


-- ------------------------------------------------------------
-- participant: someone inside a match
--   user_id NOT NULL -> registered user
--   user_id NULL     -> guest (identified by guest_token_hash)
-- ------------------------------------------------------------
CREATE TABLE participant (
                             id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
                             game_id          uuid         NOT NULL,
                             user_id          uuid,
                             name             varchar(100) NOT NULL,
                             guest_token_hash varchar(255),
                             joined_at        timestamptz  NOT NULL DEFAULT now(),

                             CONSTRAINT fk_participant_game FOREIGN KEY (game_id) REFERENCES game (id),
                             CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    -- NULLs are distinct in unique constraints, so many guests are allowed
                             CONSTRAINT uq_participant_game_user UNIQUE (game_id, user_id),
                             CONSTRAINT uq_participant_guest_token UNIQUE (guest_token_hash),
                             CONSTRAINT chk_participant_identity CHECK (user_id IS NOT NULL OR guest_token_hash IS NOT NULL)
);

CREATE INDEX idx_participant_user_id ON participant (user_id);


-- ------------------------------------------------------------
-- card: up to 3 cards per participant (MVP)
-- ------------------------------------------------------------
CREATE TABLE card (
                      id             uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
                      game_id        uuid        NOT NULL,
                      participant_id uuid        NOT NULL,
                      card_number    smallint    NOT NULL,
                      sha_256_hash   varchar(64) NOT NULL,
                      created_at     timestamptz NOT NULL DEFAULT now(),

                      CONSTRAINT fk_card_game        FOREIGN KEY (game_id)        REFERENCES game (id),
                      CONSTRAINT fk_card_participant FOREIGN KEY (participant_id) REFERENCES participant (id),
                      CONSTRAINT uq_card_participant_number UNIQUE (participant_id, card_number),
                      CONSTRAINT uq_card_game_hash          UNIQUE (game_id, sha_256_hash),
                      CONSTRAINT chk_card_number_range CHECK (card_number BETWEEN 1 AND 3)
);

-- Now that card exists, link the winner.
ALTER TABLE game
    ADD CONSTRAINT fk_game_winner_card FOREIGN KEY (winner_card_id) REFERENCES card (id);


-- ------------------------------------------------------------
-- card_cell: each cell of the 5x5 card (24 numbers + free center)
-- Columns map to B-I-N-G-O: col 0 -> 1..15, col 1 -> 16..30, ...
-- ------------------------------------------------------------
CREATE TABLE card_cell (
                           id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
                           card_id    uuid        NOT NULL,
                           number     integer,
                           row_index  smallint    NOT NULL,
                           col_index  smallint    NOT NULL,
                           is_free    boolean     NOT NULL DEFAULT false,
                           marked_at  timestamptz,

                           CONSTRAINT fk_card_cell_card FOREIGN KEY (card_id) REFERENCES card (id) ON DELETE CASCADE,
                           CONSTRAINT uq_card_cell_position UNIQUE (card_id, row_index, col_index),
                           CONSTRAINT uq_card_cell_number   UNIQUE (card_id, number),
                           CONSTRAINT chk_cell_row_range CHECK (row_index BETWEEN 0 AND 4),
                           CONSTRAINT chk_cell_col_range CHECK (col_index BETWEEN 0 AND 4),
                           CONSTRAINT chk_cell_number_by_column CHECK (
                               number IS NULL OR number BETWEEN col_index * 15 + 1 AND col_index * 15 + 15
                               ),
                           CONSTRAINT chk_cell_free_has_no_number CHECK (is_free = (number IS NULL)),
                           CONSTRAINT chk_cell_free_is_center CHECK (is_free = (row_index = 2 AND col_index = 2))
);


-- ------------------------------------------------------------
-- drawn_number: balls of a match, in call order
--   AUTOMATIC: full shuffled sequence inserted at start with
--              drawn_at NULL; each "draw next" sets drawn_at on the
--              lowest draw_order still NULL.
--   MANUAL:    rows inserted as the owner informs each number,
--              already with drawn_at set.
-- ------------------------------------------------------------
CREATE TABLE drawn_number (
                              id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
                              game_id    uuid        NOT NULL,
                              number     integer     NOT NULL,
                              draw_order smallint    NOT NULL,
                              drawn_at   timestamptz,

                              CONSTRAINT fk_drawn_number_game FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE,
                              CONSTRAINT uq_drawn_number_game_number UNIQUE (game_id, number),
                              CONSTRAINT uq_drawn_number_game_order  UNIQUE (game_id, draw_order),
                              CONSTRAINT chk_drawn_number_range CHECK (number BETWEEN 1 AND 75),
                              CONSTRAINT chk_draw_order_range   CHECK (draw_order BETWEEN 1 AND 75)
);
ALTER TABLE IF EXISTS "admin"."styled_layer"
    ADD COLUMN IF NOT EXISTS "stats_state" TEXT;
ALTER TABLE IF EXISTS "admin"."styled_layer"
    ADD COLUMN IF NOT EXISTS "activate_stats" boolean NOT NULL DEFAULT FALSE

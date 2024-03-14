ALTER TABLE IF EXISTS "admin"."style"
    ADD COLUMN IF NOT EXISTS "specification" varchar(100) NOT NULL DEFAULT 'sld';

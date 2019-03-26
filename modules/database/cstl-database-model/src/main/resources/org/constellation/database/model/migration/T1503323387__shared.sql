ALTER TABLE "admin"."metadata" ADD COLUMN "is_shared" boolean NOT NULL DEFAULT FALSE;

ALTER TABLE "admin"."style" ADD COLUMN "is_shared" boolean NOT NULL DEFAULT FALSE;
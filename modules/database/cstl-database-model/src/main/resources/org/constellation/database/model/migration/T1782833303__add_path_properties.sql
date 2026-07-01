ALTER TABLE "admin"."datasource_path" ADD COLUMN IF NOT EXISTS "modified" int8;
ALTER TABLE "admin"."datasource_path" ADD COLUMN IF NOT EXISTS "content_hash" TEXT;
ALTER TABLE "admin"."datasource_path" ADD COLUMN IF NOT EXISTS "hash_algo" TEXT;
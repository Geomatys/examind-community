
-- Remove foreign key from style to provider

ALTER TABLE "admin"."style" DROP CONSTRAINT "style_provider_fk";

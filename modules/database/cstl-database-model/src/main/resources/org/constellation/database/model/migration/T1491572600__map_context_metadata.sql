ALTER TABLE "admin"."metadata" ADD COLUMN "map_context_id" integer;
ALTER TABLE "admin"."metadata" ADD CONSTRAINT map_context_id_fk FOREIGN KEY (map_context_id) REFERENCES "admin"."mapcontext" (id);

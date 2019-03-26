
ALTER TABLE "admin"."metadata" ADD COLUMN "provider_id" integer;
ALTER TABLE "admin"."metadata" ADD CONSTRAINT metadata_provider_id_fk FOREIGN KEY (provider_id) REFERENCES "admin"."provider" (id);

SET search_path = admin, pg_catalog;

CREATE TABLE "provider_x_csw" (
    csw_id integer NOT NULL,
    provider_id integer NOT NULL,
    all_metadata boolean NOT NULL DEFAULT TRUE
);

ALTER TABLE "admin"."provider_x_csw" ADD CONSTRAINT provider_csw_cross_id_fk FOREIGN KEY (provider_id) REFERENCES "admin"."provider" (id);
ALTER TABLE "admin"."provider_x_csw" ADD CONSTRAINT csw_provider_cross_id_fk FOREIGN KEY (csw_id)      REFERENCES "admin"."service" (id);
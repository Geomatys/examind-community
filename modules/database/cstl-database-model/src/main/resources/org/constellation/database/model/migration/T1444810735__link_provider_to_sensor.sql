
ALTER TABLE "admin"."sensor" ADD COLUMN "provider_id" integer;
ALTER TABLE "admin"."sensor" ADD CONSTRAINT sensor_provider_id_fk FOREIGN KEY (provider_id) REFERENCES "admin"."provider" (id);

SET search_path = admin, pg_catalog;

CREATE TABLE "provider_x_sos" (
    sos_id integer NOT NULL,
    provider_id integer NOT NULL,
    all_sensor boolean NOT NULL DEFAULT TRUE
);

ALTER TABLE "admin"."provider_x_sos" ADD CONSTRAINT provider_sos_cross_id_fk FOREIGN KEY (provider_id) REFERENCES "admin"."provider" (id);
ALTER TABLE "admin"."provider_x_sos" ADD CONSTRAINT sos_provider_cross_id_fk FOREIGN KEY (sos_id)      REFERENCES "admin"."service" (id);


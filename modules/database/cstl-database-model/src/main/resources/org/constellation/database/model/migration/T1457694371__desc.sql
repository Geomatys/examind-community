SET search_path = admin, pg_catalog;

ALTER TABLE "sensor" DROP COLUMN "metadata";

CREATE TABLE "internal_sensor" (
    id integer NOT NULL,
    sensor_id character varying(100) NOT NULL,
    metadata text NOT NULL
);

ALTER TABLE "admin"."internal_sensor" ADD CONSTRAINT internal_sensor_id_fk FOREIGN KEY (id) REFERENCES "admin"."sensor" (id) ON DELETE CASCADE;

ALTER TABLE ONLY "admin"."internal_sensor" ADD CONSTRAINT internal_sensor_pk PRIMARY KEY (id);

CREATE SEQUENCE internal_sensor_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE internal_sensor_id_seq OWNED BY "internal_sensor"."id";

ALTER TABLE ONLY "internal_sensor" ALTER COLUMN "id" SET DEFAULT nextval('internal_sensor_id_seq'::regclass);
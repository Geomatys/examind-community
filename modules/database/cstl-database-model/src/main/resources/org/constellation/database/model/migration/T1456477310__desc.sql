SET search_path = admin, pg_catalog;

ALTER TABLE "metadata" DROP COLUMN "metadata_iso";

CREATE TABLE "internal_metadata" (
    id integer NOT NULL,
    metadata_id character varying(100) NOT NULL,
    metadata_iso text NOT NULL
);

ALTER TABLE "admin"."internal_metadata" ADD CONSTRAINT internal_metadata_id_fk FOREIGN KEY (id) REFERENCES "admin"."metadata" (id);
ALTER TABLE ONLY "admin"."internal_metadata" ADD CONSTRAINT internal_metadata_pk PRIMARY KEY (id);

CREATE SEQUENCE internal_metadata_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE internal_metadata_id_seq OWNED BY "internal_metadata"."id";

ALTER TABLE ONLY "internal_metadata" ALTER COLUMN "id" SET DEFAULT nextval('internal_metadata_id_seq'::regclass);
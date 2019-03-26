SET search_path = admin, pg_catalog;

CREATE SEQUENCE datasource_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER SEQUENCE datasource_id_seq OWNED BY "datasource"."id";

ALTER TABLE ONLY "datasource" ALTER COLUMN "id" SET DEFAULT nextval('datasource_id_seq'::regclass);
CREATE TABLE "admin"."datasource" (
    id integer NOT NULL,
    "type" character varying(50) NOT NULL,
    url character varying(1000) NOT NULL,
    username character varying(100),
    pwd character varying(500)
);

ALTER TABLE "admin"."datasource"  ADD CONSTRAINT datasource_pk PRIMARY KEY ("id");
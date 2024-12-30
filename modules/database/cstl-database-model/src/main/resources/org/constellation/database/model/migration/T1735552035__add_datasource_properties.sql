CREATE TABLE "admin"."datasource_properties" (
    "datasource_id" integer NOT NULL,
    "key" text NOT NULL,
    "value" character varying(500) NOT NULL
);

ALTER TABLE "admin"."datasource_properties"  ADD CONSTRAINT datasource_properties_pk PRIMARY KEY ("datasource_id", "key");
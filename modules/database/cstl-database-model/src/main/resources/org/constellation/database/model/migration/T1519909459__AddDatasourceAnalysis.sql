ALTER TABLE "admin"."datasource" ADD COLUMN "analysis_state" VARCHAR(50);

CREATE TABLE "admin"."datasource_store" (
    "datasource_id" integer NOT NULL,
    "store" character varying(500) NOT NULL
);

ALTER TABLE "admin"."datasource_store"  ADD CONSTRAINT datasource_store_pk PRIMARY KEY ("datasource_id", "store");
ALTER TABLE "admin"."datasource_store" ADD CONSTRAINT datasource_store_datasource_id_fk FOREIGN KEY (datasource_id) REFERENCES "admin"."datasource" (id);

CREATE TABLE "admin"."datasource_path" (
    datasource_id integer NOT NULL,
    "path" text NOT NULL,
    "name" text NOT NULL,
    "folder" boolean NOT NULL,
    "parent_path" text,
    "size" integer NOT NULL,
    "type" character varying(500)
);

ALTER TABLE "admin"."datasource_path"  ADD CONSTRAINT datasource_path_pk PRIMARY KEY ("datasource_id", "path");
ALTER TABLE "admin"."datasource_path" ADD CONSTRAINT datasource_path_datasource_id_fk FOREIGN KEY (datasource_id) REFERENCES "admin"."datasource" (id);

CREATE TABLE "admin"."datasource_path_store" (
    datasource_id integer NOT NULL,
    "path" text NOT NULL,
    "store" character varying(500) NOT NULL
);

ALTER TABLE "admin"."datasource_path_store"  ADD CONSTRAINT datasource_path_store_pk PRIMARY KEY ("datasource_id", "path", "store");
ALTER TABLE "admin"."datasource_path_store" ADD CONSTRAINT datasource_path_store_path_fk FOREIGN KEY ("datasource_id", "path") REFERENCES "admin"."datasource_path" ("datasource_id", "path");
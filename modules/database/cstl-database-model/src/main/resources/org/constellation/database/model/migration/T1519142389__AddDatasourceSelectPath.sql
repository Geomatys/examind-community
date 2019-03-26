CREATE TABLE "admin"."selected_path" (
    datasource_id integer NOT NULL,
    "path" character varying(1000) NOT NULL
);

ALTER TABLE "admin"."selected_path"  ADD CONSTRAINT selected_path_pk PRIMARY KEY ("datasource_id", "path");
ALTER TABLE "admin"."selected_path" ADD CONSTRAINT selected_path_datasource_id_fk FOREIGN KEY (datasource_id) REFERENCES "admin"."datasource" (id);
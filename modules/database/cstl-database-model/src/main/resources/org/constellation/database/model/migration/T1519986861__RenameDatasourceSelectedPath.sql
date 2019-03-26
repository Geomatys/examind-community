delete from "admin"."selected_path";
delete from "admin"."datasource_path_store";
delete from "admin"."datasource_path";
delete from "admin"."datasource_store";
delete from "admin"."datasource";

DROP TABLE "admin"."selected_path";

CREATE TABLE "admin"."datasource_selected_path" (
    datasource_id integer NOT NULL,
    "path" character varying(1000) NOT NULL
);

ALTER TABLE "admin"."datasource_selected_path"  ADD CONSTRAINT datasource_selected_path_pk PRIMARY KEY ("datasource_id", "path");
ALTER TABLE "admin"."datasource_selected_path" ADD CONSTRAINT datasource_selected_path_datasource_id_fk FOREIGN KEY (datasource_id) REFERENCES "admin"."datasource" (id);




        
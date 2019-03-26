delete from "admin"."datasource_selected_path";
delete from "admin"."datasource_path_store";
delete from "admin"."datasource_path";
delete from "admin"."datasource_store";
delete from "admin"."datasource";

ALTER TABLE "admin"."datasource_path" DROP COLUMN "type";

ALTER TABLE "admin"."datasource_path_store" DROP CONSTRAINT datasource_path_store_pk;

ALTER TABLE "admin"."datasource_path_store" ADD COLUMN "type" character varying NOT NULL;

ALTER TABLE "admin"."datasource_path_store" ADD CONSTRAINT datasource_path_store_pk PRIMARY KEY ("datasource_id", "path", "store", "type");

ALTER TABLE "admin"."datasource_store" DROP CONSTRAINT datasource_store_pk;

ALTER TABLE "admin"."datasource_store" ADD COLUMN "type" character varying NOT NULL;

ALTER TABLE "admin"."datasource_store" ADD CONSTRAINT datasource_store_pk PRIMARY KEY ("datasource_id", "store", "type");
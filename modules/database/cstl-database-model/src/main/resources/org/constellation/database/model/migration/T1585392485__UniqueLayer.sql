DROP INDEX "admin"."LAYER_NAME-SERVICE_IDX";
CREATE UNIQUE INDEX "LAYER_NAME-SERVICE_IDX" ON "admin"."layer" USING btree ("name", "namespace", "service", "alias");
ALTER TABLE "admin"."layer" DROP CONSTRAINT layer_name_uq;
ALTER TABLE ONLY "admin"."layer" ADD CONSTRAINT layer_name_uq UNIQUE ("name", "namespace", "service", "alias");
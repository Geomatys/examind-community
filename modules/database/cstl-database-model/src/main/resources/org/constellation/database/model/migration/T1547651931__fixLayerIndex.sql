SET search_path = admin, pg_catalog;
DROP INDEX "LAYER_NAME-SERVICE_IDX";
CREATE UNIQUE INDEX "LAYER_NAME-SERVICE_IDX" ON layer USING btree (name, namespace, service);
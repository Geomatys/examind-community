ALTER TABLE ONLY "admin"."scene" DROP CONSTRAINT "scene_data_fk";

ALTER TABLE ONLY "admin"."scene" DROP CONSTRAINT "scene_layer_fk";

ALTER TABLE ONLY "admin"."scene" ADD CONSTRAINT "scene_data_fk" FOREIGN KEY ("data_id") REFERENCES "admin"."data"("id") ON DELETE CASCADE;

ALTER TABLE ONLY "admin"."scene" ADD CONSTRAINT "scene_layer_fk" FOREIGN KEY ("layer_id") REFERENCES "admin"."layer"("id") ON DELETE CASCADE;
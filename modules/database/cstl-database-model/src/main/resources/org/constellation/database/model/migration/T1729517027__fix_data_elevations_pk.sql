-- Fix data elevations.
ALTER TABLE "admin"."data_elevations"
    DROP CONSTRAINT "data_elevations_pk";
ALTER TABLE "admin"."data_elevations"
    ADD CONSTRAINT "data_elevations_pk"
        PRIMARY KEY ("data_id", "elevation");
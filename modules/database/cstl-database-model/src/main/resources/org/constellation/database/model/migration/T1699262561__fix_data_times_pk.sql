-- Fix data times. Currently, only one time can be registered per data.
-- But we want to allow many, as a data can be a time-series.
-- Example: A NetCDF coverage can provide a slice per hour, day, etc.
ALTER TABLE "admin"."data_times"
    DROP CONSTRAINT "data_times_pk";
ALTER TABLE "admin"."data_times"
    ADD CONSTRAINT "data_times_pk"
        PRIMARY KEY ("data_id", "date");

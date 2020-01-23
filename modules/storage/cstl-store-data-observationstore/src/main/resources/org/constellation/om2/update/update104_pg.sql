CREATE TABLE "$SCHEMAom"."historical_locations" (
    "procedure"         character varying(200) NOT NULL,
    "time"              timestamp NOT NULL,
    "location"          geometry
);

ALTER TABLE "$SCHEMAom"."historical_locations" ADD CONSTRAINT hl_pk PRIMARY KEY ("procedure", "time");

ALTER TABLE "$SCHEMAom"."historical_locations" ADD CONSTRAINT historical_location_proc_fk FOREIGN KEY ("procedure") REFERENCES "$SCHEMAom"."procedures"("id");
UPDATE "$SCHEMAom"."version" SET "number"='1.0.4';
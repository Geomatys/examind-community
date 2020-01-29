ALTER TABLE "$SCHEMAom"."procedures" ADD "om_type" character varying(100);
UPDATE "$SCHEMAom"."version" SET "number"='1.0.5';
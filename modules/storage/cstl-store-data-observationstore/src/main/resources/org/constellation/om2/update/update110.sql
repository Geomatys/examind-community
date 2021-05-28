ALTER TABLE "$SCHEMAom"."procedures" ADD "name" character varying(200);
ALTER TABLE "$SCHEMAom"."procedures" ADD "description" character varying(1000);
UPDATE "$SCHEMAom"."version" SET "number"='1.1.0';
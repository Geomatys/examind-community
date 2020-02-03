ALTER TABLE "$SCHEMAom"."observed_properties" ADD "name" character varying(200);
ALTER TABLE "$SCHEMAom"."observed_properties" ADD "definition" character varying(200);
ALTER TABLE "$SCHEMAom"."observed_properties" ADD "description" character varying(1000);
UPDATE "$SCHEMAom"."version" SET "number"='1.0.6';

ALTER TABLE "$SCHEMAom"."observed_properties" ADD "partial" BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE "$SCHEMAom"."version" SET "number"='1.0.2';
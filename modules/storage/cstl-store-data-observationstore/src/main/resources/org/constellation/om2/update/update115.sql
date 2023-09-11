
ALTER TABLE "$SCHEMAom"."observed_properties_properties" DROP CONSTRAINT if EXISTS observed_properties_properties_pk;

ALTER TABLE "$SCHEMAom"."procedures_properties" DROP CONSTRAINT if EXISTS procedures_properties_pk;

ALTER TABLE "$SCHEMAom"."sampling_features_properties" DROP CONSTRAINT if EXISTS sampling_features_properties_pk;

UPDATE "$SCHEMAom"."version" SET "number"='1.1.5';
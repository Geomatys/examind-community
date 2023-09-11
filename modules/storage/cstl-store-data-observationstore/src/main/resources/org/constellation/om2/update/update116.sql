
ALTER TABLE "$SCHEMAom"."observed_properties_properties" ALTER COLUMN "value" SET NOT NULL;

ALTER TABLE "$SCHEMAom"."observed_properties_properties" ADD CONSTRAINT observed_properties_properties_pk PRIMARY KEY ("id_phenomenon", "property_name", "value");

ALTER TABLE "$SCHEMAom"."procedures_properties" ALTER COLUMN "value" SET NOT NULL;

ALTER TABLE "$SCHEMAom"."procedures_properties" ADD CONSTRAINT procedures_properties_pk PRIMARY KEY ("id_procedure", "property_name", "value");

ALTER TABLE "$SCHEMAom"."sampling_features_properties" ALTER COLUMN "value" SET NOT NULL;

ALTER TABLE "$SCHEMAom"."sampling_features_properties" ADD CONSTRAINT sampling_features_properties_pk PRIMARY KEY ("id_sampling_feature", "property_name", "value");

UPDATE "$SCHEMAom"."version" SET "number"='1.1.6';


CREATE TABLE "$SCHEMAom"."observed_properties_properties" (
    "id_phenomenon" character varying(200) NOT NULL,
    "property_name" character varying(200) NOT NULL,
    "value"          character varying(1000)
);

CREATE TABLE "$SCHEMAom"."procedures_properties" (
    "id_procedure" character varying(200) NOT NULL,
    "property_name" character varying(200) NOT NULL,
    "value"          character varying(1000)
);

CREATE TABLE "$SCHEMAom"."sampling_features_properties" (
    "id_sampling_feature" character varying(200) NOT NULL,
    "property_name"       character varying(200) NOT NULL,
    "value"               character varying(1000)
);

ALTER TABLE "$SCHEMAom"."observed_properties_properties" ADD CONSTRAINT observed_properties_properties_pk PRIMARY KEY ("id_phenomenon", "property_name");

ALTER TABLE "$SCHEMAom"."procedures_properties" ADD CONSTRAINT procedures_properties_pk PRIMARY KEY ("id_procedure", "property_name");

ALTER TABLE "$SCHEMAom"."sampling_features_properties" ADD CONSTRAINT sampling_features_properties_pk PRIMARY KEY ("id_sampling_feature", "property_name");

ALTER TABLE "$SCHEMAom"."observed_properties_properties" ADD CONSTRAINT observed_properties_properties_fk FOREIGN KEY ("id_phenomenon") REFERENCES "$SCHEMAom"."observed_properties"("id");

ALTER TABLE "$SCHEMAom"."procedures_properties" ADD CONSTRAINT procedures_properties_fk FOREIGN KEY ("id_procedure") REFERENCES "$SCHEMAom"."procedures"("id");

ALTER TABLE "$SCHEMAom"."sampling_features_properties" ADD CONSTRAINT sampling_features_properties_fk FOREIGN KEY ("id_sampling_feature") REFERENCES "$SCHEMAom"."sampling_features"("id");

UPDATE "$SCHEMAom"."version" SET "number"='1.1.4';
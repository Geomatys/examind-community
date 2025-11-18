CREATE SCHEMA "$SCHEMAom";
CREATE TABLE "$SCHEMAom"."version" (
    "number"   character varying(10) NOT NULL
);

INSERT INTO "$SCHEMAom"."version" VALUES ('1.1.9');

ALTER TABLE "$SCHEMAom"."version" ADD CONSTRAINT version_pk PRIMARY KEY ("number");

CREATE SCHEMA "$SCHEMAmesures";

CREATE TABLE "$SCHEMAom"."observations" (
    "identifier"        character varying(200) NOT NULL,
    "id"                integer NOT NULL,
    "time_begin"        timestamp,
    "time_end"          timestamp,
    "observed_property" character varying(200),
    "procedure"         character varying(200),
    "foi"               character varying(200)
);

CREATE TABLE "$SCHEMAom"."offerings" (
    "identifier"       character varying(100) NOT NULL,
    "description"      character varying(200),
    "name"             character varying(200),
    "time_begin"       timestamp,
    "time_end"         timestamp,
    "procedure"        character varying(200)
);

CREATE TABLE "$SCHEMAom"."offering_observed_properties" (
    "id_offering" character varying(100) NOT NULL,
    "phenomenon"  character varying(200) NOT NULL
);

CREATE TABLE "$SCHEMAom"."offering_foi" (
    "id_offering" character varying(100) NOT NULL,
    "foi"         character varying(200) NOT NULL
);

CREATE TABLE "$SCHEMAom"."observed_properties" (
    "id" character varying(200) NOT NULL,
    "partial" boolean NOT NULL DEFAULT FALSE,
    "name"        character varying(200),
    "definition"  character varying(200),
    "description" character varying(1000)
);

CREATE TABLE "$SCHEMAom"."observed_properties_properties" (
    "id_phenomenon" character varying(200) NOT NULL,
    "property_name" character varying(200) NOT NULL,
    "value"          character varying(1000) NOT NULL
);

CREATE TABLE "$SCHEMAom"."procedures" (
    "id"     character varying(200) NOT NULL,
    "shape"  geometry,
    "crs"    integer,
    "pid"    integer NOT NULL,
    "parent" character varying(200),
    "type"   character varying(200),
    "om_type" character varying(100),
    "name" character varying(200),
    "description" character varying(1000),
    "nb_table" integer NOT NULL DEFAULT 1
);

CREATE TABLE "$SCHEMAom"."procedures_properties" (
    "id_procedure" character varying(200) NOT NULL,
    "property_name" character varying(200) NOT NULL,
    "value"          character varying(1000) NOT NULL
);


CREATE TABLE "$SCHEMAom"."procedure_descriptions" (
    "procedure"         character varying(200) NOT NULL,
    "order"             integer NOT NULL,
    "field_name"        character varying(63) NOT NULL,
    "field_type"        character varying(30),
    "field_definition"  character varying(200),
    "uom"               character varying(200),
    "parent"            character varying(63),
    "table_number"      integer NOT NULL DEFAULT 1,
    "label"             character varying(500),
    "sub_field_type"    character varying(100)
);

CREATE TABLE "$SCHEMAom"."sampling_features" (
    "id"               character varying(200) NOT NULL,
    "name"             character varying(200),
    "description"      character varying(200),
    "sampledfeature"   character varying(200),
    "shape"            geometry,
    "crs"              integer
);

CREATE TABLE "$SCHEMAom"."sampling_features_properties" (
    "id_sampling_feature" character varying(200) NOT NULL,
    "property_name"       character varying(200) NOT NULL,
    "value"               character varying(1000) NOT NULL
);

CREATE TABLE "$SCHEMAom"."historical_locations" (
    "procedure"         character varying(200) NOT NULL,
    "time"              timestamp NOT NULL,
    "location"          geometry,
    "crs"               integer
);

-- USED ONLY FOR V100 SOS --

CREATE TABLE "$SCHEMAom"."components" (
    "phenomenon" character varying(200) NOT NULL,
    "component"  character varying(200) NOT NULL,
    "order"      integer
);

CREATE OR REPLACE FUNCTION "$SCHEMAmesures".getmesureidpr(z_value double precision, t timestamp without time zone)
    RETURNS bigint
    AS 'select ((extract(epoch from t)  * 1000000  + z_value * 1000) :: bigint)'
    LANGUAGE SQL;

CREATE OR REPLACE FUNCTION "$SCHEMAmesures".getmesureidts(t timestamp without time zone)
    RETURNS bigint
    AS 'select (extract(epoch from t):: bigint)'
    LANGUAGE SQL;


ALTER TABLE "$SCHEMAom"."observations" ADD CONSTRAINT observation_pk PRIMARY KEY ("id");

ALTER TABLE "$SCHEMAom"."offerings" ADD CONSTRAINT offering_pk PRIMARY KEY ("identifier");

ALTER TABLE "$SCHEMAom"."offering_observed_properties" ADD CONSTRAINT offering_op_pk PRIMARY KEY ("id_offering", "phenomenon");

ALTER TABLE "$SCHEMAom"."offering_foi" ADD CONSTRAINT offering_foi_pk PRIMARY KEY ("id_offering", "foi");

ALTER TABLE "$SCHEMAom"."observed_properties" ADD CONSTRAINT observed_properties_pk PRIMARY KEY ("id");

ALTER TABLE "$SCHEMAom"."procedures" ADD CONSTRAINT procedure_pk PRIMARY KEY ("id");

ALTER TABLE "$SCHEMAom"."procedure_descriptions" ADD CONSTRAINT procedure_descriptions_uq UNIQUE ("procedure", "field_name", "parent", "sub_field_type");

ALTER TABLE "$SCHEMAom"."sampling_features" ADD CONSTRAINT sf_pk PRIMARY KEY ("id");

ALTER TABLE "$SCHEMAom"."components" ADD CONSTRAINT components_op_pk PRIMARY KEY ("phenomenon", "component");

ALTER TABLE "$SCHEMAom"."observed_properties_properties" ADD CONSTRAINT observed_properties_properties_pk PRIMARY KEY ("id_phenomenon", "property_name", "value");

ALTER TABLE "$SCHEMAom"."procedures_properties" ADD CONSTRAINT procedures_properties_pk PRIMARY KEY ("id_procedure", "property_name", "value");

ALTER TABLE "$SCHEMAom"."sampling_features_properties" ADD CONSTRAINT sampling_features_properties_pk PRIMARY KEY ("id_sampling_feature", "property_name", "value");

ALTER TABLE "$SCHEMAom"."procedure_descriptions" ADD CONSTRAINT procedure_desc_fk FOREIGN KEY ("procedure") REFERENCES "$SCHEMAom"."procedures"("id");

ALTER TABLE "$SCHEMAom"."observations" ADD CONSTRAINT observation_op_fk FOREIGN KEY ("observed_property") REFERENCES "$SCHEMAom"."observed_properties"("id");

ALTER TABLE "$SCHEMAom"."observations" ADD CONSTRAINT observation_procedure_fk FOREIGN KEY ("procedure") REFERENCES "$SCHEMAom"."procedures"("id");

ALTER TABLE "$SCHEMAom"."observations" ADD CONSTRAINT observation_foi_fk FOREIGN KEY ("foi") REFERENCES "$SCHEMAom"."sampling_features"("id");

ALTER TABLE "$SCHEMAom"."offerings" ADD CONSTRAINT offering_procedure_fk FOREIGN KEY ("procedure") REFERENCES "$SCHEMAom"."procedures"("id");

ALTER TABLE "$SCHEMAom"."offering_observed_properties" ADD CONSTRAINT offering_op_off_fk FOREIGN KEY ("id_offering") REFERENCES "$SCHEMAom"."offerings"("identifier");

ALTER TABLE "$SCHEMAom"."offering_observed_properties" ADD CONSTRAINT offering_op_op_fk FOREIGN KEY ("phenomenon") REFERENCES "$SCHEMAom"."observed_properties"("id");

ALTER TABLE "$SCHEMAom"."offering_foi" ADD CONSTRAINT offering_foi_off_fk FOREIGN KEY ("id_offering") REFERENCES "$SCHEMAom"."offerings"("identifier");

ALTER TABLE "$SCHEMAom"."offering_foi" ADD CONSTRAINT offering_foi_foi_fk FOREIGN KEY ("foi") REFERENCES "$SCHEMAom"."sampling_features"("id");

ALTER TABLE "$SCHEMAom"."components" ADD CONSTRAINT component_base_fk FOREIGN KEY ("phenomenon") REFERENCES "$SCHEMAom"."observed_properties"("id");

ALTER TABLE "$SCHEMAom"."components" ADD CONSTRAINT component_child_fk FOREIGN KEY ("component") REFERENCES "$SCHEMAom"."observed_properties"("id");

ALTER TABLE "$SCHEMAom"."historical_locations" ADD CONSTRAINT hl_pk PRIMARY KEY ("procedure", "time");

ALTER TABLE "$SCHEMAom"."historical_locations" ADD CONSTRAINT historical_location_proc_fk FOREIGN KEY ("procedure") REFERENCES "$SCHEMAom"."procedures"("id");

ALTER TABLE "$SCHEMAom"."observed_properties_properties" ADD CONSTRAINT observed_properties_properties_fk FOREIGN KEY ("id_phenomenon") REFERENCES "$SCHEMAom"."observed_properties"("id");

ALTER TABLE "$SCHEMAom"."procedures_properties" ADD CONSTRAINT procedures_properties_fk FOREIGN KEY ("id_procedure") REFERENCES "$SCHEMAom"."procedures"("id");

ALTER TABLE "$SCHEMAom"."sampling_features_properties" ADD CONSTRAINT sampling_features_properties_fk FOREIGN KEY ("id_sampling_feature") REFERENCES "$SCHEMAom"."sampling_features"("id");

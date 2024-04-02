INSTALL spatial;
LOAD spatial;

CREATE SCHEMA "$SCHEMAom";

CREATE TABLE "$SCHEMAom"."version" (
    "number"   character varying(10) PRIMARY KEY
);

INSERT INTO "$SCHEMAom"."version" VALUES ('1.1.7');

CREATE SCHEMA "$SCHEMAmesures";


CREATE TABLE "$SCHEMAom"."observed_properties" (
    "id" character varying(200) PRIMARY KEY,
    "partial" integer NOT NULL DEFAULT 0,
    "name"        character varying(200),
    "definition"  character varying(200),
    "description" character varying(1000)
);

CREATE TABLE "$SCHEMAom"."observed_properties_properties" (
    "id_phenomenon" character varying(200) NOT NULL REFERENCES "$SCHEMAom"."observed_properties"("id"),
    "property_name" character varying(200) NOT NULL,
    "value"          character varying(1000) NOT NULL,
    PRIMARY KEY ("id_phenomenon", "property_name", "value")
);

CREATE TABLE "$SCHEMAom"."procedures" (
    "id"     character varying(200) PRIMARY KEY,
    "shape"  GEOMETRY,
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
    "id_procedure" character varying(200) NOT NULL REFERENCES "$SCHEMAom"."procedures"("id"),
    "property_name" character varying(200) NOT NULL,
    "value"          character varying(1000) NOT NULL,
    PRIMARY KEY ("id_procedure", "property_name", "value")
);

CREATE TABLE "$SCHEMAom"."procedure_descriptions" (
    "procedure"         character varying(200) REFERENCES "$SCHEMAom"."procedures"("id"),
    "order"             integer NOT NULL,
    "field_name"        character varying(63) NOT NULL,
    "field_type"        character varying(30),
    "field_definition"  character varying(200),
    "uom"               character varying(20),
    "parent"            character varying(63),
    "table_number"      integer NOT NULL DEFAULT 1,
    "label"             character varying(500),
    UNIQUE ("procedure", "field_name", "parent")
);

CREATE TABLE "$SCHEMAom"."sampling_features" (
    "id"               character varying(200) PRIMARY KEY,
    "name"             character varying(200),
    "description"      character varying(200),
    "sampledfeature"   character varying(200),
    "shape"            GEOMETRY,
    "crs"              integer
);

CREATE TABLE "$SCHEMAom"."sampling_features_properties" (
    "id_sampling_feature" character varying(200) NOT NULL  REFERENCES "$SCHEMAom"."sampling_features"("id"),
    "property_name"       character varying(200) NOT NULL,
    "value"               character varying(1000) NOT NULL,
    PRIMARY KEY ("id_sampling_feature", "property_name", "value")
);

CREATE TABLE "$SCHEMAom"."historical_locations" (
    "procedure"         character varying(200) NOT NULL REFERENCES "$SCHEMAom"."procedures"("id"),
    "time"              timestamp NOT NULL,
    "location"          GEOMETRY,
    "crs"               integer,
    PRIMARY KEY ("procedure", "time")
);

-- USED ONLY FOR V100 SOS --

CREATE TABLE "$SCHEMAom"."components" (
    "phenomenon" character varying(200) NOT NULL REFERENCES "$SCHEMAom"."observed_properties"("id"),
    "component"  character varying(200) NOT NULL REFERENCES "$SCHEMAom"."observed_properties"("id"),
    "order"      integer,
    PRIMARY KEY ("phenomenon", "component")
);

CREATE TABLE "$SCHEMAom"."offerings" (
    "identifier"       character varying(100) PRIMARY KEY,
    "description"      character varying(200),
    "name"             character varying(200),
    "time_begin"       timestamp,
    "time_end"         timestamp,
    "procedure"        character varying(200) REFERENCES "$SCHEMAom"."procedures"("id")
);

CREATE TABLE "$SCHEMAom"."offering_observed_properties" (
    "id_offering" character varying(100) NOT NULL REFERENCES "$SCHEMAom"."offerings"("identifier"),
    "phenomenon"  character varying(200) NOT NULL REFERENCES "$SCHEMAom"."observed_properties"("id"),
    PRIMARY KEY ("id_offering", "phenomenon")
);

CREATE TABLE "$SCHEMAom"."offering_foi" (
    "id_offering" character varying(100) NOT NULL REFERENCES "$SCHEMAom"."offerings"("identifier"),
    "foi"         character varying(200) NOT NULL REFERENCES "$SCHEMAom"."sampling_features"("id"),
    PRIMARY KEY ("id_offering", "foi")
);

CREATE TABLE "$SCHEMAom"."observations" (
    "identifier"        character varying(200) NOT NULL,
    "id"                integer PRIMARY KEY,
    "time_begin"        timestamp,
    "time_end"          timestamp,
    "observed_property" character varying(200) REFERENCES "$SCHEMAom"."observed_properties"("id"),
    "procedure"         character varying(200) REFERENCES "$SCHEMAom"."procedures"("id"),
    "foi"               character varying(200) REFERENCES "$SCHEMAom"."sampling_features"("id")
);

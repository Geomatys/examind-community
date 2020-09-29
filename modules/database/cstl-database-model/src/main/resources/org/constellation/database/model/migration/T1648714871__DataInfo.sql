ALTER TABLE "admin"."data" ADD COLUMN "cached_info" boolean NOT NULL DEFAULT FALSE;

ALTER TABLE "admin"."data" ADD COLUMN "has_time" boolean NOT NULL DEFAULT FALSE;
ALTER TABLE "admin"."data" ADD COLUMN "has_elevation" boolean NOT NULL DEFAULT FALSE;
ALTER TABLE "admin"."data" ADD COLUMN "has_dim" boolean NOT NULL DEFAULT FALSE;

ALTER TABLE "admin"."data" ADD COLUMN "crs" character varying(100000);

CREATE TABLE "admin"."data_envelope" (
    "data_id" integer NOT NULL,
    "dimension" integer NOT NULL,
    "min" double precision NOT NULL,
    "max" double precision NOT NULL
);

ALTER TABLE ONLY "admin"."data_envelope" ADD CONSTRAINT "data_envelope_pk" PRIMARY KEY ("data_id", "dimension");

ALTER TABLE ONLY "admin"."data_envelope" ADD CONSTRAINT "data_envelope_data_fk" FOREIGN KEY ("data_id") REFERENCES "admin"."data"("id") ON DELETE CASCADE;

CREATE TABLE "admin"."data_dim_range" (
    "data_id" integer NOT NULL,
    "dimension" integer NOT NULL,
    "min" double precision NOT NULL,
    "max" double precision NOT NULL,
    "unit" character varying(1000),
    "unit_symbol" character varying(1000)
);

ALTER TABLE ONLY "admin"."data_dim_range" ADD CONSTRAINT "data_dim_range_pk" PRIMARY KEY ("data_id", "dimension");

ALTER TABLE ONLY "admin"."data_dim_range" ADD CONSTRAINT "data_dim_range_data_fk" FOREIGN KEY ("data_id") REFERENCES "admin"."data"("id") ON DELETE CASCADE;

CREATE TABLE "admin"."data_times" (
    "data_id" integer NOT NULL,
    "date" bigint NOT NULL
);

ALTER TABLE ONLY "admin"."data_times" ADD CONSTRAINT "data_times_pk" PRIMARY KEY ("data_id");

ALTER TABLE ONLY "admin"."data_times" ADD CONSTRAINT "data_times_data_fk" FOREIGN KEY ("data_id") REFERENCES "admin"."data"("id") ON DELETE CASCADE;

CREATE TABLE "admin"."data_elevations" (
    "data_id" integer NOT NULL,
    "elevation" double precision NOT NULL
);

ALTER TABLE ONLY "admin"."data_elevations" ADD CONSTRAINT "data_elevations_pk" PRIMARY KEY ("data_id");

ALTER TABLE ONLY "admin"."data_elevations" ADD CONSTRAINT "data_elevations_data_fk" FOREIGN KEY ("data_id") REFERENCES "admin"."data"("id") ON DELETE CASCADE;
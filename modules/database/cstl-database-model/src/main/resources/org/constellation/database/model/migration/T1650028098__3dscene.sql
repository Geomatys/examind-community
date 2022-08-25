CREATE TABLE "admin"."theater" (
    "id" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
    "name" character varying(10000) NOT NULL,
    "data_id" integer NOT NULL,
    "layer_id" integer,
    "type" character varying(100)
);

ALTER TABLE ONLY "admin"."theater" ADD CONSTRAINT "theater_pk" PRIMARY KEY ("id");

ALTER TABLE ONLY "admin"."theater" ADD CONSTRAINT "theater_data_fk" FOREIGN KEY ("layer_id") REFERENCES "admin"."layer"("id") ON DELETE CASCADE;

ALTER TABLE ONLY "admin"."theater" ADD CONSTRAINT "theater_layer_fk" FOREIGN KEY ("data_id") REFERENCES "admin"."data"("id") ON DELETE CASCADE;

ALTER TABLE ONLY "admin"."theater" ADD CONSTRAINT "theater_name_unique_key" UNIQUE ("name");


CREATE TABLE "admin"."scene" (
    "id" integer NOT NULL,
    "name" character varying(10000) NOT NULL,
    "map_context_id" integer NOT NULL,
    "data_id" integer,
    "layer_id" integer,
    "type" character varying(100),
    "surface" integer ARRAY,
    "surface_parameters" character varying(10000),
    "surface_factor" double precision,
    "status" character varying(100) NOT NULL,
    "creation_date" bigint NOT NULL,
    "min_lod" integer,
    "max_lod" integer,
    "bbox_minx" double precision,
    "bbox_miny" double precision,
    "bbox_maxx" double precision,
    "bbox_maxy" double precision,
    "time" character varying(10000),
    "extras" character varying(10000),
    "vector_simplify_factor" double precision
);

ALTER TABLE ONLY "admin"."scene" ADD CONSTRAINT "scene_pk" PRIMARY KEY ("id");

ALTER TABLE ONLY "admin"."scene" ADD CONSTRAINT "scene_data_fk" FOREIGN KEY ("layer_id") REFERENCES "admin"."data"("id") ON DELETE CASCADE;

ALTER TABLE ONLY "admin"."scene" ADD CONSTRAINT "scene_layer_fk" FOREIGN KEY ("data_id") REFERENCES "admin"."layer"("id") ON DELETE CASCADE;

ALTER TABLE ONLY "admin"."scene" ADD CONSTRAINT "scene_name_unique_key" UNIQUE ("name");

CREATE TABLE "admin"."theater_scene" (
    "theater_id" integer NOT NULL,
    "scene_id" integer NOT NULL
);

ALTER TABLE ONLY "admin"."theater_scene" ADD CONSTRAINT "theater_scene_pk" PRIMARY KEY ("theater_id", "scene_id");

ALTER TABLE ONLY "admin"."theater_scene" ADD CONSTRAINT "theater_scene_scene_fk" FOREIGN KEY ("scene_id") REFERENCES "admin"."scene"("id") ON DELETE CASCADE;

ALTER TABLE ONLY "admin"."theater_scene" ADD CONSTRAINT "theater_scene_theather_fk" FOREIGN KEY ("theater_id") REFERENCES "admin"."theater"("id") ON DELETE CASCADE;
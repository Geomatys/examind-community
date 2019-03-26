SET search_path = admin, pg_catalog;

CREATE TABLE "thesaurus" (
    id            integer NOT NULL,
    uri           character varying(200) NOT NULL,
    "name"        character varying(200) NOT NULL,
    description   character varying(500),
    creation_date bigint  NOT NULL,
    "state"       boolean NOT NULL DEFAULT TRUE,
    defaultLang   character varying(3),
    version       character varying(20),
    schemaName    character varying(100)
);

CREATE SEQUENCE thesaurus_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE thesaurus_id_seq OWNED BY thesaurus.id;

ALTER TABLE "admin"."thesaurus" ALTER COLUMN id SET DEFAULT nextval('thesaurus_id_seq'::regclass);

ALTER TABLE "admin"."thesaurus"  ADD CONSTRAINT thesaurus_pk PRIMARY KEY (id);

CREATE TABLE "thesaurus_language" (
    thesaurus_id integer NOT NULL,
    "language"   character varying(3)
);

ALTER TABLE "admin"."thesaurus_language"  ADD CONSTRAINT thesaurus_language_pk PRIMARY KEY (thesaurus_id, "language");
ALTER TABLE "admin"."thesaurus_language" ADD CONSTRAINT thesaurus_language_fk FOREIGN KEY (thesaurus_id) REFERENCES "admin"."thesaurus" (id);


CREATE TABLE "thesaurus_x_service" (
    service_id integer NOT NULL,
    thesaurus_id integer NOT NULL
);

ALTER TABLE "admin"."thesaurus_x_service" ADD CONSTRAINT thesaurus_service_cross_id_fk FOREIGN KEY (thesaurus_id) REFERENCES "admin"."thesaurus" (id);
ALTER TABLE "admin"."thesaurus_x_service" ADD CONSTRAINT service_thesaurus_cross_id_fk FOREIGN KEY (service_id)   REFERENCES "admin"."service" (id);

CREATE SCHEMA "th_base";

CREATE TABLE "th_base"."term_count" (
    "label"            character varying(250) NOT NULL,
    "service"          int NOT NULL,
    "language"         character varying(2) NOT NULL,
    "count"            int NOT NULL,
    "aggregated_count" int NOT NULL,
    "theme"            character varying(100),
    "uri_concept"      character varying(250) NOT NULL,
    "uri_thesaurus"    character varying(250) NOT NULL
);

CREATE TABLE "th_base"."aggregated_identifier" (
    "label"       character varying(250) NOT NULL,
    "service"     int NOT NULL,
    "identifier"  character varying NOT NULL,
    "uri_concept" character varying(250) NOT NULL,
    "language"    character varying(2) NOT NULL,
    "uri_thesaurus"    character varying(250) NOT NULL
);

CREATE TABLE "th_base"."linked_service" (
    "id"  int NOT NULL,
    "url" character varying(100) NOT NULL
);

ALTER TABLE "th_base"."term_count" ADD CONSTRAINT "pk_term_count" PRIMARY KEY ("uri_concept", "label", "service" , "language");
ALTER TABLE "th_base"."aggregated_identifier" ADD CONSTRAINT "pk_aggregated_identifier" PRIMARY KEY ("uri_concept", "label", "service", "language");
ALTER TABLE "th_base"."linked_service" ADD CONSTRAINT "pk_linked_service" PRIMARY KEY ("id");
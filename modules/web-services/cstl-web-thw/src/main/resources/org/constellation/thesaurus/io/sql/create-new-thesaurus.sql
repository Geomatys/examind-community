CREATE SCHEMA "{schema}";

CREATE TABLE "{schema}"."propriete_thesaurus" (
    "uri"       character varying(150) NOT NULL,
    "name"      character varying(150) NOT NULL,
    "description" character varying(1000),
    "defaultLang" character varying(2),
    "enable"    integer NOT NULL
);

CREATE TABLE "{schema}"."language" (
    "language_iso" character varying(2) NOT NULL,
    "enable"      integer NOT NULL
);

CREATE TABLE "{schema}"."propriete_concept" (
    "uri_concept" character varying(250) NOT NULL,
    "predicat"    character varying(250) NOT NULL,
    "objet"       character varying(100000) NOT NULL,
    "graphid"     integer
);

CREATE TABLE "{schema}"."terme_completion" (
    "uri_concept"       character varying(250) NOT NULL,
    "label"             character varying(250) NOT NULL,
    "thesaurus_origine" character varying(50),
    "langage_iso"       character(2) NOT NULL,
    "type_terme"        character varying(250) NOT NULL
);

CREATE TABLE "{schema}"."terme_localisation" (
    "uri_concept"       character varying(250) NOT NULL,
    "label"             character varying(10000) NOT NULL,
    "thesaurus_origine" character varying(50),
    "langage_iso"       character(2) NOT NULL,
    "type_terme"        character varying(250) NOT NULL
);

ALTER TABLE "{schema}"."propriete_thesaurus" ADD CONSTRAINT "pk_propriete_thesaurus" PRIMARY KEY ("uri");

ALTER TABLE "{schema}"."language" ADD CONSTRAINT "pk_language" PRIMARY KEY ("language_iso");

ALTER TABLE "{schema}"."propriete_concept" ADD CONSTRAINT "pk_propriete_concept" PRIMARY KEY ("uri_concept", "predicat", "objet");

ALTER TABLE "{schema}"."terme_completion" ADD CONSTRAINT "pk_terme_completion" PRIMARY KEY ("uri_concept", "label", "type_terme", "langage_iso");

ALTER TABLE "{schema}"."terme_localisation" ADD CONSTRAINT "pk_terme_localisation" PRIMARY KEY ("uri_concept", "label", "type_terme", "langage_iso");

CREATE INDEX "objet_idx" ON "{schema}"."propriete_concept" USING btree ("objet");
CREATE INDEX "uri_concept_idx" ON "{schema}"."propriete_concept" USING btree ("uri_concept");
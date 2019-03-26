CREATE SCHEMA "$schema";

CREATE TABLE "$schema"."records"(
  "identifier"  VARCHAR(128) NOT NULL UNIQUE,
  "path"  VARCHAR(1024)      NOT NULL
);

ALTER TABLE "$schema"."records" ADD CONSTRAINT records_pk PRIMARY KEY ("identifier","path");

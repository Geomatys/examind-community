ALTER TABLE "$SCHEMAom"."procedures" ADD "nb_table" integer NOT NULL DEFAULT 1;
ALTER TABLE "$SCHEMAom"."procedure_descriptions" ADD "table_number" integer NOT NULL DEFAULT 1;
UPDATE "$SCHEMAom"."version" SET "number"='1.1.3';
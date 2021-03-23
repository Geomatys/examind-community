ALTER TABLE "$SCHEMAom"."procedure_descriptions" ALTER COLUMN "field_name" TYPE character varying(63);
UPDATE "$SCHEMAom"."version" SET "number"='1.0.9';

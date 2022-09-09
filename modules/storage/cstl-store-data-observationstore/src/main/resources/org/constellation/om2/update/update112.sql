ALTER TABLE "$SCHEMAom"."procedure_descriptions" DROP CONSTRAINT procedure_descriptions_pk;
ALTER TABLE "$SCHEMAom"."procedure_descriptions" ADD CONSTRAINT procedure_descriptions_uq UNIQUE ("procedure", "field_name", "parent");
UPDATE "$SCHEMAom"."version" SET "number"='1.1.2';


/**
 * Author:  glegal
 * Created: 10 févr. 2025
 */
ALTER TABLE "$SCHEMAom"."procedure_descriptions" ADD COLUMN "sub_field_type" character varying(100);

ALTER TABLE "$SCHEMAom"."procedure_descriptions" DROP CONSTRAINT procedure_descriptions_uq;
ALTER TABLE "$SCHEMAom"."procedure_descriptions" ADD CONSTRAINT procedure_descriptions_uq UNIQUE ("procedure", "field_name", "parent", "sub_field_type");

UPDATE "$SCHEMAom"."version" SET "number"='1.1.8';

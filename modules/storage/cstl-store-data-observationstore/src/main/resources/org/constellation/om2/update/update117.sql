
/**
 * Author:  glegal
 * Created: 29 mars 2024
 */
ALTER TABLE "$SCHEMAom"."procedure_descriptions" ADD COLUMN "label" character varying(500);

UPDATE "$SCHEMAom"."version" SET "number"='1.1.7';
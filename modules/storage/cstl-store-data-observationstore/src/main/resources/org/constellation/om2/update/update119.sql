/**
 * Author:  glegal
 * Created: 28 avr. 2025
 */
ALTER TABLE "$SCHEMAom"."procedure_descriptions" ALTER COLUMN "uom" TYPE character varying(200);

UPDATE "$SCHEMAom"."version" SET "number"='1.1.9';
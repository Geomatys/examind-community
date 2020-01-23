ALTER TABLE "om"."procedures" ADD "parent" character varying(200);
ALTER TABLE "om"."procedures" ADD "type"   character varying(200);
UPDATE "$SCHEMAom"."version" SET "number"='1.0.3';


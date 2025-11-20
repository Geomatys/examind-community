
CREATE OR REPLACE FUNCTION "$SCHEMAmesures".getmesureidpr(z_value, t)
    AS (select EPOCH(t::TIMESTAMP)::BIGINT  * 1000000  + z_value * 1000);

CREATE OR REPLACE FUNCTION "$SCHEMAmesures".getmesureidts(t)
    AS (select EPOCH(t::TIMESTAMP));

UPDATE "$SCHEMAom"."version" SET "number"='1.2.0';
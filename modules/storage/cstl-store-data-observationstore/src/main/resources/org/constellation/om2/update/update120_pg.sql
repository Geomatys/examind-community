
CREATE OR REPLACE FUNCTION "$SCHEMAmesures".getmesureidpr(z_value double precision, t timestamp without time zone)
    RETURNS bigint
    AS 'select ((extract(epoch from t)  * 1000000  + z_value * 1000) :: bigint)'
    LANGUAGE SQL;

CREATE OR REPLACE FUNCTION "$SCHEMAmesures".getmesureidts(t timestamp without time zone)
    RETURNS bigint
    AS 'select (extract(epoch from t):: bigint)'
    LANGUAGE SQL;


UPDATE "$SCHEMAom"."version" SET "number"='1.2.0';
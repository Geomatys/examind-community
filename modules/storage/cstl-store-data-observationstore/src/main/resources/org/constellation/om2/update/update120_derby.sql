
CREATE OR REPLACE FUNCTION "$SCHEMAmesures".getmesureidpr
( z_value DOUBLE, t TIMESTAMP )
RETURNS BIGINT
PARAMETER STYLE JAVA
NO SQL LANGUAGE JAVA
EXTERNAL NAME 'org.constellation.store.observation.db.mixed.DerbyFunctions.getMesureIdPr';

CREATE OR REPLACE FUNCTION "$SCHEMAmesures".getmesureidts
(t TIMESTAMP )
RETURNS BIGINT
PARAMETER STYLE JAVA
NO SQL LANGUAGE JAVA
EXTERNAL NAME 'org.constellation.store.observation.db.mixed.DerbyFunctions.getMesureIdTs';
UPDATE "$SCHEMAom"."version" SET "number"='1.2.0';
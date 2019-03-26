CREATE TABLE "admin"."sensor_x_sos" (
    sensor_id integer NOT NULL,
    sos_id integer NOT NULL
);
ALTER TABLE "admin"."sensor_x_sos" ADD CONSTRAINT sensor_x_sos_pk PRIMARY KEY (sensor_id, sos_id);

ALTER TABLE "admin"."sensor_x_sos" ADD CONSTRAINT sensor_sos_cross_id_fk FOREIGN KEY (sensor_id) REFERENCES "admin"."sensor" (id);
ALTER TABLE "admin"."sensor_x_sos" ADD CONSTRAINT sos_sensor_cross_id_fk FOREIGN KEY (sos_id)    REFERENCES "admin"."service" (id);
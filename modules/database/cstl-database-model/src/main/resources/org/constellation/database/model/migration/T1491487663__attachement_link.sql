ALTER TABLE "admin"."attachment" ADD COLUMN "uri" character varying(500);

CREATE TABLE "admin"."metadata_x_attachment" (
    "attachement_id" integer NOT NULL,
    "metadata_id" integer NOT NULL
);

ALTER TABLE "admin"."metadata_x_attachment"  ADD CONSTRAINT metadata_x_attachment_pk PRIMARY KEY ("attachement_id", "metadata_id");
ALTER TABLE "admin"."metadata_x_attachment" ADD CONSTRAINT metadata_attachment_cross_id_fk FOREIGN KEY ("attachement_id") REFERENCES "admin"."attachment" (id);
ALTER TABLE "admin"."metadata_x_attachment" ADD CONSTRAINT attachment_metadata_cross_id_fk FOREIGN KEY ("metadata_id")    REFERENCES "admin"."metadata" (id);
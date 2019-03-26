-- Add byte array field to store a attachements (used for metadata quicklook)

CREATE TABLE "admin"."attachment" (
    id integer NOT NULL,
    content bytea NOT NULL
);

CREATE SEQUENCE "admin"."attachment_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE "admin"."attachment_id_seq" OWNED BY "admin"."attachment"."id";
ALTER TABLE ONLY "admin"."attachment" ALTER COLUMN id SET DEFAULT nextval('admin.attachment_id_seq'::regclass);
ALTER TABLE ONLY "admin"."attachment" ADD CONSTRAINT attachment_pk PRIMARY KEY (id);

-- Switch context to the default Pluggable Database (FREEPDB1)
ALTER SESSION SET CONTAINER = FREEPDB1;

-- Create the user 'ktor'
CREATE USER ktor IDENTIFIED BY ktor
  DEFAULT TABLESPACE users
  TEMPORARY TABLESPACE temp
  QUOTA UNLIMITED ON users;

-- Grant required privileges and roles
GRANT CONNECT, RESOURCE, DBA TO ktor;
GRANT ALL PRIVILEGES TO ktor;

CREATE SEQUENCE ktor.persons_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE ktor.persons
(
    id      NUMBER(10) DEFAULT ktor.persons_id_seq.NEXTVAL
        CONSTRAINT pk_persons PRIMARY KEY,
    name    VARCHAR2(100 CHAR)
        CONSTRAINT nn_persons_name NOT NULL,
    age     NUMBER(10)
        CONSTRAINT nn_persons_age NOT NULL,
    details JSON
        CONSTRAINT nn_persons_details NOT NULL
);

-- ----------------------------------------------------------------------------
-- Optional: enforce the expected shape of the "details" JSON document
-- (PersonDetails: address, phoneNumber), leveraging Oracle 26ai JSON Schema
-- validation support.
-- ----------------------------------------------------------------------------
ALTER TABLE ktor.persons
    ADD CONSTRAINT chk_persons_details_schema
        CHECK (
            details IS JSON VALIDATE
            '{
                "type": "object",
                "properties": {
                    "address": { "type": "string" },
                    "phoneNumber": { "type": "string" }
                },
                "required": ["address", "phoneNumber"]
            }'
        );

-- ----------------------------------------------------------------------------
-- Comments
-- ----------------------------------------------------------------------------
COMMENT ON TABLE ktor.persons IS 'Persons managed by the application';
COMMENT ON COLUMN ktor.persons.id IS 'Surrogate primary key, auto-generated identity';
COMMENT ON COLUMN ktor.persons.name IS 'Person full name';
COMMENT ON COLUMN ktor.persons.age IS 'Person age in years';
COMMENT ON COLUMN ktor.persons.details IS 'JSON document holding PersonDetails: address, phoneNumber';

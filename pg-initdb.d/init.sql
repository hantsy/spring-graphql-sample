CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- There is no 'if not exists' for type
-- But use a 'Drop' will cause exception if the type is applied in table schema.
-- DROP TYPE post_status;

-- The value only accept single quotes.
-- CREATE TYPE post_status AS ENUM( 'DRAFT', 'PENDING_MODERATION', 'PUBLISHED');

-- A simple way to skip exception when creating type if it is existed.
-- DO $$ BEGIN
--    CREATE TYPE post_status AS ENUM( 'DRAFT', 'PENDING_MODERATION', 'PUBLISHED');
-- EXCEPTION
--    WHEN duplicate_object THEN null;
-- END $$;
-- Use EnumCodec to handle enum between Java and pg. 
-- see: https://github.com/pgjdbc/r2dbc-postgresql#postgres-enum-types
-- CREATE CAST (varchar AS post_status) WITH INOUT AS IMPLICIT;

-- In sprig data jdbc/r2dbc, varchar is converted to Java enum automaticially. No need create a enum type in postgres.

CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
	email VARCHAR(255) NOT NULL,
	password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP ,
    version INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS posts (
    -- id SERIAL PRIMARY KEY,
    id UUID DEFAULT uuid_generate_v4(),
    title VARCHAR(255),
    content VARCHAR(255),
    status VARCHAR(255) DEFAULT 'DRAFT', 
	author_id UUID REFERENCES users ,
    created_at TIMESTAMP, --NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at TIMESTAMP,
    version INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS comments (
    id UUID DEFAULT uuid_generate_v4(),
    content VARCHAR(255),
    post_id UUID REFERENCES posts ON DELETE CASCADE,
    created_at TIMESTAMP ,
    version INTEGER,
    PRIMARY KEY (id)
);



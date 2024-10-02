CREATE DATABASE board;
\c board

CREATE TABLE jobs(
    id UUID DEFAULT gen_random_uuid(),
    date BIGINT NOT NULL,
    ownerEmail TEXT NOT NULL,
    company TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    externalUrl TEXT NOT NULL,
    remote BOOLEAN NOT NULL DEFAULT false,
    salaryLo INTEGER,
    salaryHi INTEGER,
    currency TEXT,
    location TEXT NOT NULL,
    country TEXT,
    tags TEXT[],
    seniority TEXT,
    other TEXT,
    active BOOLEAN NOT NULL DEFAULT false
);

ALTER TABLE jobs ADD CONSTRAINT pk_jobs PRIMARY KEY (id);
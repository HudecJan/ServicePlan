CREATE TABLE monthly_schedule (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    "year" INT NOT NULL,
    "month" INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    generated_at TIMESTAMP,
    CONSTRAINT uk_schedule_year_month UNIQUE ("year", "month")
);

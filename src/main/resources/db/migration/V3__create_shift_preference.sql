CREATE TABLE shift_preference (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    date DATE NOT NULL,
    preference_type VARCHAR(30) NOT NULL,
    note VARCHAR(500),
    CONSTRAINT fk_preference_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT uk_preference_doctor_date UNIQUE (doctor_id, date)
);

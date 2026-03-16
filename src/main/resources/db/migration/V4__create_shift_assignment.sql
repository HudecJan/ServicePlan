CREATE TABLE shift_assignment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    date DATE NOT NULL,
    shift_type VARCHAR(30) NOT NULL,
    forced BOOLEAN NOT NULL DEFAULT FALSE,
    warning VARCHAR(500),
    CONSTRAINT fk_assignment_schedule FOREIGN KEY (schedule_id) REFERENCES monthly_schedule(id),
    CONSTRAINT fk_assignment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT uk_assignment_schedule_date_type UNIQUE (schedule_id, date, shift_type)
);

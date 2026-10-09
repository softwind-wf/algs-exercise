BEGIN;

CREATE SCHEMA IF NOT EXISTS er_fig_7_23_demo;

DROP TABLE IF EXISTS er_fig_7_23_demo.eval_for CASCADE;
DROP TABLE IF EXISTS er_fig_7_23_demo.evaluation CASCADE;
DROP TABLE IF EXISTS er_fig_7_23_demo.proj_guide CASCADE;
DROP TABLE IF EXISTS er_fig_7_23_demo.project CASCADE;
DROP TABLE IF EXISTS er_fig_7_23_demo.student CASCADE;
DROP TABLE IF EXISTS er_fig_7_23_demo.instructor CASCADE;

CREATE TABLE er_fig_7_23_demo.instructor (
    instructor_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    salary NUMERIC(12,2) NOT NULL CHECK (salary >= 0)
);

CREATE TABLE er_fig_7_23_demo.student (
    student_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    total_credits INT NOT NULL DEFAULT 0 CHECK (total_credits >= 0)
);

CREATE TABLE er_fig_7_23_demo.project (
    project_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title TEXT NOT NULL,
    budget NUMERIC(14,2) NOT NULL CHECK (budget >= 0)
);

CREATE TABLE er_fig_7_23_demo.proj_guide (
    instructor_id BIGINT NOT NULL
        REFERENCES er_fig_7_23_demo.instructor(instructor_id),
    student_id BIGINT NOT NULL
        REFERENCES er_fig_7_23_demo.student(student_id),
    project_id BIGINT NOT NULL
        REFERENCES er_fig_7_23_demo.project(project_id),
    PRIMARY KEY (student_id, project_id, instructor_id)
);

CREATE TABLE er_fig_7_23_demo.evaluation (
    evaluation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    score NUMERIC(5,2) CHECK (score BETWEEN 0 AND 100),
    comments TEXT,
    evaluated_on DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE er_fig_7_23_demo.eval_for (
    evaluation_id BIGINT PRIMARY KEY
        REFERENCES er_fig_7_23_demo.evaluation(evaluation_id),
    instructor_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    FOREIGN KEY (student_id, project_id, instructor_id)
        REFERENCES er_fig_7_23_demo.proj_guide
            (student_id, project_id, instructor_id)
);

INSERT INTO er_fig_7_23_demo.instructor (name, salary) VALUES
    ('Zhang Wei', 85000),
    ('Li Na', 92000);

INSERT INTO er_fig_7_23_demo.student (name, total_credits) VALUES
    ('Chen Hao', 98),
    ('Wang Fang', 105);

INSERT INTO er_fig_7_23_demo.project (title, budget) VALUES
    ('AI Knowledge Base', 50000),
    ('Campus Navigation', 35000);

INSERT INTO er_fig_7_23_demo.proj_guide
    (instructor_id, student_id, project_id)
SELECT i.instructor_id, s.student_id, p.project_id
FROM (VALUES
    ('Zhang Wei', 'Chen Hao', 'AI Knowledge Base'),
    ('Zhang Wei', 'Wang Fang', 'Campus Navigation')
) AS guide(instructor_name, student_name, project_title)
JOIN er_fig_7_23_demo.instructor i
    ON i.name = guide.instructor_name
JOIN er_fig_7_23_demo.student s
    ON s.name = guide.student_name
JOIN er_fig_7_23_demo.project p
    ON p.title = guide.project_title;

INSERT INTO er_fig_7_23_demo.evaluation (score, comments, evaluated_on) VALUES
    (94.50, 'Excellent prototype and clear documentation.', DATE '2026-10-01'),
    (87.00, 'Good implementation, improve testing.', DATE '2026-10-05');

INSERT INTO er_fig_7_23_demo.eval_for
    (evaluation_id, instructor_id, student_id, project_id)
SELECT e.evaluation_id, g.instructor_id, g.student_id, g.project_id
FROM (VALUES
    ('Excellent prototype and clear documentation.',
     'Zhang Wei', 'Chen Hao', 'AI Knowledge Base'),
    ('Good implementation, improve testing.',
     'Zhang Wei', 'Wang Fang', 'Campus Navigation')
) AS input(comment_text, instructor_name, student_name, project_title)
JOIN er_fig_7_23_demo.evaluation e
    ON e.comments = input.comment_text
JOIN er_fig_7_23_demo.proj_guide g
    ON true
JOIN er_fig_7_23_demo.instructor i
    ON i.instructor_id = g.instructor_id
JOIN er_fig_7_23_demo.student s
    ON s.student_id = g.student_id
JOIN er_fig_7_23_demo.project p
    ON p.project_id = g.project_id
WHERE i.name = input.instructor_name
  AND s.name = input.student_name
  AND p.title = input.project_title;

DO $$
BEGIN
    INSERT INTO er_fig_7_23_demo.eval_for
        (evaluation_id, instructor_id, student_id, project_id)
    VALUES (999999, 999999, 999999, 999999);
    RAISE EXCEPTION 'Invalid aggregate insert unexpectedly succeeded';
EXCEPTION
    WHEN foreign_key_violation THEN
        RAISE NOTICE 'Constraint demo passed: an evaluation cannot reference a nonexistent proj_guide aggregate.';
END $$;

\echo '--- proj_guide rows ---'
SELECT i.name AS instructor, s.name AS student, p.title AS project
FROM er_fig_7_23_demo.proj_guide g
JOIN er_fig_7_23_demo.instructor i ON i.instructor_id = g.instructor_id
JOIN er_fig_7_23_demo.student s ON s.student_id = g.student_id
JOIN er_fig_7_23_demo.project p ON p.project_id = g.project_id
ORDER BY p.title, s.name;

\echo '--- evaluations linked through eval_for ---'
SELECT e.evaluation_id, e.score, e.comments, e.evaluated_on,
       i.name AS instructor, s.name AS student, p.title AS project
FROM er_fig_7_23_demo.evaluation e
JOIN er_fig_7_23_demo.eval_for ef
    ON ef.evaluation_id = e.evaluation_id
JOIN er_fig_7_23_demo.proj_guide g
    ON g.student_id = ef.student_id
   AND g.project_id = ef.project_id
   AND g.instructor_id = ef.instructor_id
JOIN er_fig_7_23_demo.instructor i
    ON i.instructor_id = g.instructor_id
JOIN er_fig_7_23_demo.student s
    ON s.student_id = g.student_id
JOIN er_fig_7_23_demo.project p
    ON p.project_id = g.project_id
ORDER BY e.evaluation_id;

COMMIT;

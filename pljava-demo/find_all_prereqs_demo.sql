\set ON_ERROR_STOP off
DROP TABLE IF EXISTS prereq;

-- 关系 prereq(course_id, prereq_id)：course_id 需要 prereq_id
-- 构造传递链验证"间接先修"：CS-315 依赖 CS-101/CS-200，而 CS-101 又依赖 CS-050
CREATE TABLE prereq (course_id varchar(8), prereq_id varchar(8), PRIMARY KEY (course_id, prereq_id));
INSERT INTO prereq VALUES
  ('CS-315', 'CS-101'),
  ('CS-315', 'CS-200'),
  ('CS-101', 'CS-050');

-- 写法一：递归 CTE（PostgreSQL 最地道，直接做传递闭包）
CREATE OR REPLACE FUNCTION find_all_prereqs_rec(cid varchar(8))
RETURNS TABLE (course_id varchar(8)) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE closure(course_id) AS (
        SELECT prereq.prereq_id FROM prereq WHERE prereq.course_id = cid
        UNION
        SELECT p.prereq_id
        FROM prereq p
        JOIN closure c ON c.course_id = p.course_id
    )
    SELECT closure.course_id FROM closure;
END;
$$ LANGUAGE plpgsql;

-- 写法二：PL/pgSQL 循环版（忠实还原教材 repeat...until 迭代算法）
CREATE OR REPLACE FUNCTION find_all_prereqs_loop(cid varchar(8))
RETURNS TABLE (course_id varchar(8)) AS $$
BEGIN
    CREATE TEMP TABLE c_prereq (course_id varchar(8)) ON COMMIT DROP;
    CREATE TEMP TABLE new_c_prereq (course_id varchar(8)) ON COMMIT DROP;
    CREATE TEMP TABLE temp (course_id varchar(8)) ON COMMIT DROP;

    INSERT INTO new_c_prereq
      SELECT prereq.prereq_id FROM prereq WHERE prereq.course_id = cid;

    LOOP
        INSERT INTO c_prereq
          SELECT n.course_id FROM new_c_prereq n;

        INSERT INTO temp
          SELECT p2.prereq_id
          FROM new_c_prereq n, prereq p2
          WHERE n.course_id = p2.course_id
            AND p2.prereq_id NOT IN (SELECT cp.course_id FROM c_prereq cp);

        DELETE FROM new_c_prereq;
        INSERT INTO new_c_prereq SELECT t.course_id FROM temp t;
        DELETE FROM temp;

        EXIT WHEN NOT EXISTS (SELECT 1 FROM new_c_prereq);
    END LOOP;

    RETURN QUERY SELECT cp.course_id FROM c_prereq cp;
END;
$$ LANGUAGE plpgsql;

\echo '===== 递归 CTE 版: find_all_prereqs_rec(''CS-315'') 应返 {CS-101, CS-050, CS-200} ====='
SELECT course_id FROM find_all_prereqs_rec('CS-315') ORDER BY course_id;

\echo '===== 循环版: find_all_prereqs_loop(''CS-315'') 应与上面一致 ====='
SELECT course_id FROM find_all_prereqs_loop('CS-315') ORDER BY course_id;

\echo '===== 无先修课程: find_all_prereqs_rec(''CS-050'') 应为空 ====='
SELECT course_id FROM find_all_prereqs_rec('CS-050') ORDER BY course_id;

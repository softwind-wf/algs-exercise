-- ============================================================
-- PostgreSQL 可调用语句（Callable Statement）演示
--
-- 建库:  psql.bat -d postgres "CREATE DATABASE callable_demo"
-- 执行:  psql.bat -d callable_demo -f src\main\resources\sql\callable_demo_pg.sql
--        （psql.bat 现在转发官方 psql.exe, $$ 函数体、注释里的分号、-v ON_ERROR_STOP=1 都支持;
--          旧版 psql.bat 走 JDBC 切分器时这里会报 Unterminated dollar quote）
-- 实测: 用真实 psql 执行 exit 0 无报错, employee 3 行, it_employees() 正常返回。
--
-- 演示对象（供 JDBC CallableStatement 调用）:
--   1. 标量函数      dept_count(VARCHAR) -> INT     返回单个值
--   2. OUT 参数函数  emp_info(...) -> 3 个 OUT 值    一次返回多个值
--   3. 表返回函数    it_employees() -> TABLE        返回一行或多行结果集
--   4. 存储过程      apply_raise(...) 带 INOUT      修改数据并把新值回传
--
-- 对照: 函数能嵌在 SELECT 表达式里； 存储过程只能 CALL。
-- ============================================================

-- ---------- 演示用表 ----------
DROP TABLE IF EXISTS employee;
CREATE TABLE employee (
    emp_id   SERIAL PRIMARY KEY,
    emp_name VARCHAR(50) NOT NULL,
    dept     VARCHAR(30) NOT NULL,
    salary   NUMERIC(10,2) NOT NULL
);

INSERT INTO employee (emp_name, dept, salary) VALUES
    ('Alice', 'IT', 8000.00),
    ('Bob',   'HR', 6500.00),
    ('Carol', 'IT', 9500.00);

-- ---------- 1. 标量函数：IN 参数，返回单个值 ----------
CREATE OR REPLACE FUNCTION dept_count(dept_name VARCHAR)
RETURNS INTEGER AS $$
DECLARE cnt INTEGER;
BEGIN
    SELECT COUNT(*) INTO cnt FROM employee WHERE dept = dept_name;
    RETURN cnt;
END;
$$ LANGUAGE plpgsql;

-- ---------- 2. OUT 参数函数：一次返回多个值 ----------
CREATE OR REPLACE FUNCTION emp_info(emp_id_in INTEGER,
                                    OUT emp_name VARCHAR,
                                    OUT dept VARCHAR,
                                    OUT salary NUMERIC)
AS $$
BEGIN
    SELECT e.emp_name, e.dept, e.salary
      INTO emp_name, dept, salary
      FROM employee e WHERE e.emp_id = emp_id_in;
END;
$$ LANGUAGE plpgsql;

-- ---------- 3. 表返回函数：返回一行或多行结果集 ----------
CREATE OR REPLACE FUNCTION it_employees()
RETURNS TABLE (emp_id INTEGER, emp_name VARCHAR, salary NUMERIC)
AS $$
BEGIN
    RETURN QUERY SELECT e.emp_id, e.emp_name, e.salary
                   FROM employee e
                  WHERE e.dept = 'IT'
                  ORDER BY e.emp_id;
END;
$$ LANGUAGE plpgsql;

-- ---------- 4. 存储过程：CALL 调用，支持 INOUT ----------
-- (PostgreSQL 11+ 才有 PROCEDURE； 更早只能写"无返回值函数")
CREATE OR REPLACE PROCEDURE apply_raise(emp_id_in INTEGER,
                                        pct NUMERIC,
                                        INOUT new_salary NUMERIC)
AS $$
BEGIN
    UPDATE employee SET salary = salary * (1 + pct / 100)
     WHERE emp_id = emp_id_in;
    SELECT salary INTO new_salary FROM employee WHERE emp_id = emp_id_in;
END;
$$ LANGUAGE plpgsql;

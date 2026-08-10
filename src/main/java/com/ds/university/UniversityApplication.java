package com.ds.university;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 大学网站启动类。
 * 数据库：university（先执行 src/main/resources/sql/university.sql 与 university_auth.sql）。
 */
@SpringBootApplication
@MapperScan("com.ds.university.mapper")
public class UniversityApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniversityApplication.class, args);
    }
}
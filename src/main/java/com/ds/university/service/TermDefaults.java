package com.ds.university.service;

import com.ds.university.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;

/**
 * 学期/年份默认值：请求未带参数时的兜底取值。
 * 年份取数据库中开课班的最大年份（不再硬编码示例数据值），
 * section 表为空时回退为当前年份；学期默认值可通过配置项覆盖。
 */
@Component
public class TermDefaults {

    private final StudentMapper studentMapper;
    private final String defaultSemester;

    public TermDefaults(StudentMapper studentMapper,
                        @Value("${university.default-semester:Spring}") String defaultSemester) {
        this.studentMapper = studentMapper;
        this.defaultSemester = defaultSemester;
    }

    /** 默认学期（配置项 university.default-semester，缺省 Spring） */
    public String semester() {
        return defaultSemester;
    }

    /** 默认年份：有开课班时取最大年份，否则取当前年份 */
    public int year() {
        List<Integer> years = studentMapper.selectDistinctYears();
        if (years != null && !years.isEmpty()) {
            // selectDistinctYears 已按年份从新到旧排序，首个即最大值
            return years.get(0);
        }
        return Year.now().getValue();
    }
}

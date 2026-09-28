package com.example.aicodeassistant.mapper;

import com.example.aicodeassistant.entity.ReviewReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 代码审查报告 Mapper。
 */
@Mapper
public interface ReviewReportMapper {

    @Insert("INSERT INTO review_report(task_id, file_name, severity, issue_type, line_number, description, "
            + "suggestion, created_at) "
            + "VALUES(#{taskId}, #{fileName}, #{severity}, #{issueType}, #{lineNumber}, #{description}, "
            + "#{suggestion}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReviewReport report);

    @Select("SELECT * FROM review_report WHERE task_id = #{taskId} ORDER BY created_at ASC")
    List<ReviewReport> findByTaskId(Long taskId);
}

package com.github.blackbk.sqlboost.uac;

import org.springframework.core.Ordered;

/**
 * @Author Kai Yi
 * @Date 2019/11/28
 * @Description 在更新到targetTable表前对要更新的值做转换
 * SourceTable --> {@link AfterGetFromSourceConverter} --> {@link BeforeUpdateTargetConverter} --> TargetTable
 */

public interface BeforeUpdateTargetConverter extends Ordered {

    /**
     * 指定该converter作用于目标表的表名（targetTable）
     *
     * @return
     */
    String table();

    /**
     * 指定该converter作用于目标表的更新列名（updateColumn）
     *
     * @return
     */
    String column();

    /**
     * convert value
     *
     * @param value       Convert sourceTable's sourceColumn value
     * @param sourceTable 当前阶段下的source规则(来源于配置)
     * @param targetTable 当前阶段下的target规则(来源于配置)
     * @return
     */
    Object convert(Object value, SourceTable sourceTable, TargetTable targetTable);

}

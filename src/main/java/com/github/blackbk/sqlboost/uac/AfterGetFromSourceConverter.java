package com.github.blackbk.sqlboost.uac;


import org.springframework.core.Ordered;

/**
 * @Author Kai Yi
 * @Date 2019/11/29
 * @Description 从sourceTable表中获取值后对值进行转换
 * SourceTable --> {@link AfterGetFromSourceConverter} --> {@link BeforeUpdateTargetConverter} --> TargetTable
 */

public interface AfterGetFromSourceConverter extends Ordered {


    /**
     * 指定该converter作用于源表的表名（sourceTable）
     *
     * @return
     */
    String table();

    /**
     * 指定该converter作用于源表的数据来源列名（sourceColumn）
     *
     * @return
     */
    String column();

    /**
     * convert value
     *
     * @param value       from sourceTable's sourceColumn
     * @param sourceTable 当前阶段下的source规则(来源于配置)
     * @return
     */
    Object convert(Object value, SourceTable sourceTable);

}

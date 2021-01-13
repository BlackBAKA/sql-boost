package com.github.blackbk.sqlboost.uac;


import com.github.blackbk.sqlboost.property.UacProperty;

import java.util.*;

/**
 * @Author Kai Yi
 * @Date 2019/11/23
 * @Description
 */

public class Rule {

    private final Map<SourceTable, Set<TargetTable>> ruleMap = new HashMap<>();

    private final Map<SourceKey, Set<SourceTable>> sourceRuleMap = new HashMap<>();

    private final Map<String, Set<String>> sourceTabColMap = new HashMap<>();

    private static class SourceKey {
        private final String table;
        private final String sourceColumn;

        SourceKey(String table, String sourceColumn) {
            this.table = table;
            this.sourceColumn = sourceColumn;
        }

        String getTable() {
            return table;
        }

        String getSourceColumn() {
            return sourceColumn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SourceKey sourceKey = (SourceKey) o;
            return Objects.equals(table, sourceKey.table) &&
                    Objects.equals(sourceColumn, sourceKey.sourceColumn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(table, sourceColumn);
        }
    }

    public Rule(UacProperty uacProperty) {
        if (uacProperty == null || uacProperty.getRules() == null || uacProperty.getRules().isEmpty()) {
            return;
        }
        for (UacProperty.SourceProperty s : uacProperty.getRules()) {
            if (s.getAssociation() == null || s.getAssociation().isEmpty()) {
                continue;
            }
            SourceTable key = new SourceTable(s.getSourceTable(),s.getPrimaryColumn(), s.getRootColumn(), s.getSourceColumn());
            if (!ruleMap.containsKey(key)) {
                ruleMap.put(key, new HashSet<>());
            }
            Set<TargetTable> value = ruleMap.get(key);
            for (UacProperty.TargetProperty t : s.getAssociation()) {
                TargetTable target = new TargetTable(t.getTargetTable(), t.getAssoColumn(), t.getTargetColumn());
                value.add(target);
            }
        }
        for (SourceTable target : ruleMap.keySet()) {
            SourceKey key = new SourceKey(target.getTable(), target.getSourceColumn());
            if (!sourceRuleMap.containsKey(key)) {
                sourceRuleMap.put(key, new HashSet<>());
            }
            sourceRuleMap.get(key).add(target);
        }
        for (SourceKey sourceKey : sourceRuleMap.keySet()) {
            String key = sourceKey.getTable();
            if (!sourceTabColMap.containsKey(key)) {
                sourceTabColMap.put(key, new HashSet<>());
            }
            sourceTabColMap.get(key).add(sourceKey.getSourceColumn());
        }
    }

    /**
     * 表是否有关联更新规则，适用于source表
     *
     * @param table
     * @return
     */
    public boolean constainsAssociation(String table) {
        return sourceTabColMap.containsKey(table);
    }

    /**
     * 表是否有关联更新规则，适用于source表
     *
     * @param table
     * @param column
     * @return
     */
    public boolean constainsAssociation(String table, String column) {
        return sourceRuleMap.containsKey(new SourceKey(table, column));
    }

    /**
     * 获取表关联更新的字段
     *
     * @param table
     * @return
     */
    public Set<String> getSourceColumn(String table) {
        return sourceTabColMap.get(table);
    }

    /**
     * 判断是否存在该source规则
     *
     * @param table
     * @param column
     * @return
     */
    public boolean constainsSourceRule(String table, String column) {
        return sourceRuleMap.containsKey(new SourceKey(table, column));
    }

    /**
     * 根据表名、字段获取source规则
     *
     * @param table
     * @param column
     * @return
     */
    public Set<SourceTable> getSourceRule(String table, String column) {
        return sourceRuleMap.get(new SourceKey(table, column));
    }


    /**
     * 判断是否存在该target规则
     *
     * @param sourceTable
     * @return
     */
    public boolean constainsTargetRule(SourceTable sourceTable) {
        return ruleMap.containsKey(sourceTable);
    }


    /**
     * 根据表名、源索引字段、源数据字段获取target规则
     *
     * @param sourceTable
     * @return
     */
    public Set<TargetTable> getTargetRule(SourceTable sourceTable) {
        return ruleMap.get(sourceTable);
    }

}

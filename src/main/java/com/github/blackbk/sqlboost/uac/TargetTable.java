package com.github.blackbk.sqlboost.uac;

import java.util.Objects;

/**
 * @Author Kai Yi
 * @Date 2019/11/25
 * @Description
 */

public class TargetTable {

    private final String table;

    private final String assoColumn;

    private final String targetColumn;


    public TargetTable(String table, String assoColumn, String targetColumn) {
        this.table = table;
        this.assoColumn = assoColumn;
        this.targetColumn = targetColumn;
    }

    public String getTable() {
        return table;
    }

    public String getAssoColumn() {
        return assoColumn;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetTable that = (TargetTable) o;
        return Objects.equals(table, that.table) &&
                Objects.equals(assoColumn, that.assoColumn) &&
                Objects.equals(targetColumn, that.targetColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, assoColumn, targetColumn);
    }
}

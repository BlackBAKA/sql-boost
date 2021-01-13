package com.github.blackbk.sqlboost.uac;

import java.util.Objects;

/**
 * @Author Kai Yi
 * @Date 2019/11/25
 * @Description
 */

public class SourceTable {

    private final String table;
    //不计算hashcode
    private final String primaryColumn;

    private final String rootColumn;

    private final String sourceColumn;


    public SourceTable(String table, String primaryColumn, String rootColumn, String sourceColumn) {
        this.table = table;
        this.primaryColumn = primaryColumn;
        this.rootColumn = rootColumn;
        this.sourceColumn = sourceColumn;
    }

    public String getTable() {
        return table;
    }

    public String getPrimaryColumn() {
        return primaryColumn;
    }

    public String getRootColumn() {
        return rootColumn;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceTable that = (SourceTable) o;
        return Objects.equals(table, that.table) &&
                Objects.equals(rootColumn, that.rootColumn) &&
                Objects.equals(sourceColumn, that.sourceColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, rootColumn, sourceColumn);
    }
}

package com.github.blackbk.sqlboost.uac;

import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @Author Kai Yi
 * @Date 2019/11/26
 * @Description
 */

public class UpdateParam {

    private String tableName;

    private String updateColumn;

    private Object updateValue;

    private String whereColumn;

    private Object whereValue;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(@NotNull @NotEmpty String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new NullPointerException("UpdateParam setter error: tableName is null or empty.");
        }
        this.tableName = tableName;
    }


    public String getUpdateColumn() {
        return updateColumn;
    }

    public void setUpdateColumn(@NotNull @NotEmpty String updateColumn) {
        if (StringUtils.isBlank(updateColumn)) {
            throw new NullPointerException("UpdateParam setter error: updateColumn is null or empty.");
        }
        this.updateColumn = updateColumn;
    }

    public Object getUpdateValue() {
        return updateValue;
    }

    public void setUpdateValue(Object updateValue) {
        this.updateValue = updateValue;
    }

    public String getWhereColumn() {
        return whereColumn;
    }

    public void setWhereColumn(@NotNull @NotEmpty String whereColumn) {
        if (StringUtils.isBlank(whereColumn)) {
            throw new NullPointerException("UpdateParam setter error: whereColumn is null or empty.");
        }
        this.whereColumn = whereColumn;
    }

    public Object getWhereValue() {
        return whereValue;
    }

    public void setWhereValue(Object whereValue) {
        if (whereValue == null) {
            throw new NullPointerException("UpdateParam setter error: whereValue is null.");
        }
        this.whereValue = whereValue;
    }
}

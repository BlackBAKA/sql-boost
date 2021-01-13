package com.github.blackbk.sqlboost.property;

import java.util.Arrays;

/**
 * @Author Kai Yi
 * @Date 2019/12/06
 * @Description
 */


public class TimeAppenderProperty {

    private boolean enable;

    private String createTimeColumn;

    private String updateTimeColumn;

    private String[] excludeTables;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getCreateTimeColumn() {
        return createTimeColumn;
    }

    public void setCreateTimeColumn(String createTimeColumn) {
        this.createTimeColumn = createTimeColumn;
    }

    public String getUpdateTimeColumn() {
        return updateTimeColumn;
    }

    public void setUpdateTimeColumn(String updateTimeColumn) {
        this.updateTimeColumn = updateTimeColumn;
    }

    public String[] getExcludeTables() {
        return excludeTables;
    }

    public void setExcludeTables(String[] excludeTables) {
        this.excludeTables = excludeTables;
    }

    @Override
    public String toString() {
        return "TimeAppenderProperty{" +
                "enable=" + enable +
                ", createTimeColumn='" + createTimeColumn + '\'' +
                ", updateTimeColumn='" + updateTimeColumn + '\'' +
                ", excludeTables=" + Arrays.toString(excludeTables) +
                '}';
    }
}

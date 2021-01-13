package com.github.blackbk.sqlboost.property;

import java.util.List;

/**
 * @Author Kai Yi
 * @Date 2019/11/22
 * @Description
 */

public class UacProperty {

    private boolean enable;

    private List<SourceProperty> rules;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public List<SourceProperty> getRules() {
        return rules;
    }

    public void setRules(List<SourceProperty> rules) {
        this.rules = rules;
    }

    @Override
    public String toString() {
        return "UacProperty{" +
                "enable=" + enable +
                ", rules=" + rules +
                '}';
    }

    public static class SourceProperty {

        private String sourceTable;

        private String sourceColumn;

        private String rootColumn;

        private String primaryColumn;

        private List<TargetProperty> association;

        public String getSourceTable() {
            return sourceTable;
        }

        public void setSourceTable(String sourceTable) {
            this.sourceTable = sourceTable;
        }

        public String getSourceColumn() {
            return sourceColumn;
        }

        public void setSourceColumn(String sourceColumn) {
            this.sourceColumn = sourceColumn;
        }

        public List<TargetProperty> getAssociation() {
            return association;
        }

        public void setAssociation(List<TargetProperty> association) {
            this.association = association;
        }

        public String getRootColumn() {
            return rootColumn;
        }

        public void setRootColumn(String rootColumn) {
            this.rootColumn = rootColumn;
        }

        public String getPrimaryColumn() {
            return primaryColumn;
        }

        public void setPrimaryColumn(String primaryColumn) {
            this.primaryColumn = primaryColumn;
        }

        @Override
        public String toString() {
            return "SourceProperty{" +
                    "sourceTable='" + sourceTable + '\'' +
                    ", sourceColumn='" + sourceColumn + '\'' +
                    ", rootColumn='" + rootColumn + '\'' +
                    ", primaryColumn='" + primaryColumn + '\'' +
                    ", association=" + association +
                    '}';
        }
    }

    public static class TargetProperty {

        private String targetTable;

        private String targetColumn;

        private String assoColumn;

        public String getAssoColumn() {
            return assoColumn;
        }

        public void setAssoColumn(String assoColumn) {
            this.assoColumn = assoColumn;
        }

        public String getTargetTable() {
            return targetTable;
        }

        public void setTargetTable(String targetTable) {
            this.targetTable = targetTable;
        }

        public String getTargetColumn() {
            return targetColumn;
        }

        public void setTargetColumn(String targetColumn) {
            this.targetColumn = targetColumn;
        }

        @Override
        public String toString() {
            return "TargetProperty{" +
                    "targetTable='" + targetTable + '\'' +
                    ", targetColumn='" + targetColumn + '\'' +
                    ", assoColumn='" + assoColumn + '\'' +
                    '}';
        }
    }

}

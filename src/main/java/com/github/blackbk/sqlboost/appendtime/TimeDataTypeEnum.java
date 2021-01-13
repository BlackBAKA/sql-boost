package com.github.blackbk.sqlboost.appendtime;

/**
 * @Author Kai Yi
 * @Date 2019/12/07
 * @Description mysql日期相关数据类型
 */

public enum TimeDataTypeEnum {
    DATE("date"),
    TIMESTAMP("timestamp"),
    TIME("time"),
    YEAR("year"),
    DATETIME("datetime");

    private String type;

    TimeDataTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }


    public static TimeDataTypeEnum getEnum(String type) {
        if (type == null) {
            return null;
        }
        for (TimeDataTypeEnum e : TimeDataTypeEnum.values()) {
            if (e.getType().equalsIgnoreCase(type)) {
                return e;
            }
        }
        return null;
    }

}

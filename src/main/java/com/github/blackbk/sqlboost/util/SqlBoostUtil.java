package com.github.blackbk.sqlboost.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Kai Yi
 * @Date 2020/04/17
 * @Description
 */

public class SqlBoostUtil {


    /**
     * 清除show create table 返回的表结构sql中多余的语句（如表结构信息中的注释、索引、主键）
     * JSqlParser v3.1版本不能解析 注释、存在comment的索引、索引描述包含USING BTREE 的表结构sql
     *
     * @return
     */
    public static String pureCreateTableSql(String sql) {
        if (StringUtils.isBlank(sql)) {
            return "";
        }
        List<String> frags = Arrays.stream(sql.split("\n"))
                .map(t -> StringUtils.replaceOnceIgnoreCase(t, "USING BTREE", " ").trim())
                .collect(Collectors.toList());

        int last = 0;
        for (int i = 0; i < frags.size(); i++) {
            String f = frags.get(i);
            if (StringUtils.isBlank(f)) {
                continue;
            }
            boolean skip = StringUtils.startsWithAny(f, "#", "--", "/*", "KEY", "INDEX", "UNIQUE");
            // 若被忽略的行不是以","为末尾，则需要将上一行末尾的","去除，以符合表结构sql格式要求
            if (skip) {
                if (!StringUtils.endsWith(f, ",") && i >= 1 && StringUtils.endsWith(frags.get(last), ",")) {
                    String pre = frags.get(last);
                    frags.set(last, pre.substring(0, pre.length() - 1));
                }
                frags.set(i, "");
            } else {
                last = i;
            }
        }

        StringBuilder sqlBuilder = new StringBuilder();
        frags.forEach(t -> sqlBuilder.append(t).append("\n"));
        return sqlBuilder.toString().trim();
    }


}

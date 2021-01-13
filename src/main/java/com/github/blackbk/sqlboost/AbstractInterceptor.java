package com.github.blackbk.sqlboost;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

import javax.validation.constraints.NotNull;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * @Author Kai Yi
 * @Date 2019/10/14
 * @Description 模板类，包含mybatis拦截器常用公共方法
 */

public abstract class AbstractInterceptor {

    // 默认日期格式化 (线程安全)
    private final FastDateFormat defaultFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取sql语句，并替换sql中的?
     *
     * @param configuration
     * @param boundSql
     * @return
     */
    protected String extractSql(@NotNull Configuration configuration, @NotNull BoundSql boundSql) {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(boundSql);
        // 获取参数
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql();
        if (parameterMappings != null && !parameterMappings.isEmpty() && parameterObject != null) {
            // 获取类型处理器注册器，类型处理器的功能是进行java类型和数据库类型的转换
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            // 如果根据parameterObject.getClass(）可以找到对应的类型，则替换
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?",
                        Matcher.quoteReplacement(getParameterValue(parameterObject)));

            } else {
                // MetaObject主要是封装了originalObject对象，提供了get和set的方法用于获取和设置originalObject的属性值,主要支持对JavaBean、Collection、Map三种类型对象的操作
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?",
                                Matcher.quoteReplacement(getParameterValue(obj)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        // 该分支是动态sql
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?",
                                Matcher.quoteReplacement(getParameterValue(obj)));
                    } else {
                        // 打印出缺失，提醒该参数缺失并防止错位
                        sql = sql.replaceFirst("\\?", "缺失");
                    }
                }
            }
        }
        return sql.trim();
    }


    /**
     * 如果参数是String，则添加单引号， 如果是日期，则转换为时间格式器并加单引号； 对参数是null和不是null的情况作了处理
     *
     * @param obj
     * @return
     */
    protected String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            value = "'" + defaultFormat.format((Date) obj) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        return value;
    }


}

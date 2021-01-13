package com.github.blackbk.sqlboost.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @Author Kai Yi
 * @Date 2019/11/27
 * @Description
 */

public interface CheckPropertyMapper {


    @Select("select 1 from ${tableName} where false")
    List<Map<String, Object>> checkTable(@NotNull @NotBlank @Param("tableName") String tableName);


    @Select("select ${columnName} from ${tableName} where false")
    List<Map<String, Object>> checkColumn(@NotNull @NotBlank @Param("tableName") String tableName,
                                          @NotNull @NotBlank @Param("columnName") String columnName);

    @Select("show create table ${tableName}")
    Map<String, String> showTable(@NotNull @NotBlank @Param("tableName") String tableName);
}

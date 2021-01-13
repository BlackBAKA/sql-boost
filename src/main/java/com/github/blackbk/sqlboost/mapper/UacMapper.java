package com.github.blackbk.sqlboost.mapper;

import com.github.blackbk.sqlboost.uac.UpdateParam;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Author Kai Yi
 * @Date 2019/11/26
 * @Description
 */


public interface UacMapper {

    /**
     * 从source表中取出条件匹配的行记录主键列表
     *
     * @param tableName
     * @param tableAlias
     * @param primaryColumn
     * @param whereSql
     * @return
     */
    @Select("<script>" +
            "select ${primaryColumn} " +
            "from ${tableName} " +
            "<if test=\"tableAlias!=null and tableAlias!=''\">" +
            "   ${tableAlias} " +
            "</if> " +
            "<choose>" +
            "    <when test=\"whereSql != null and whereSql!=''\">" +
            "      where ${whereSql}" +
            "    </when>" +
            "    <otherwise>" +
            "    </otherwise>" +
            "</choose>" +
            "</script>")
    List<Object> listPrimary(@NotNull @NotBlank @Param("tableName") String tableName,
                             @Param("tableAlias") String tableAlias, @NotNull @NotBlank @Param("primaryColumn") String primaryColumn,
                             @Param("whereSql") String whereSql);

    /**
     * 从source表中取出数据
     *
     * @param tableName
     * @param selectColumns
     * @param primaryColumn
     * @param primaryList
     * @return
     */
    @Select("<script>" +
            "select " +
            "<foreach collection=\"selectColumns\" separator=\",\" index=\"index\" item=\"item\">" +
            "  ${item}" +
            "</foreach>" +
            "from ${tableName} " +
            "where ${primaryColumn} in (" +
            "<foreach collection=\"primaryList\" separator=\",\" index=\"index\" item=\"item\">" +
            "  #{item}" +
            "</foreach>" +
            ") " +
            "group by " +
            "<foreach collection=\"selectColumns\" separator=\",\" index=\"index\" item=\"item\">" +
            "  ${item}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> selectFromSource(@NotNull @NotBlank @Param("tableName") String tableName,
                                               @NotNull @NotEmpty @Param("selectColumns") Collection<String> selectColumns, @Param("primaryColumn") String primaryColumn,
                                               @NotNull @NotEmpty @Param("primaryList") Collection<Object> primaryList);

    /**
     * 批量向target中更新数据
     *
     * @param updateParamList
     * @return
     */
    @Update("<script>" +
            "<foreach collection=\"updateParamList\" index=\"index\" item=\"updateParam\">" +
            "   update ${updateParam.tableName} " +
            "   <choose>" +
            "       <when test=\"updateParam.updateValue != null\">" +
            "           set ${updateParam.updateColumn} = #{updateParam.updateValue} " +
            "       </when>" +
            "       <otherwise>" +
            "           set ${updateParam.updateColumn} = null " +
            "       </otherwise>" +
            "   </choose>" +
            "   where ${updateParam.whereColumn} = #{updateParam.whereValue} " + ";" +
            "</foreach> " +
            "</script>")
    int batchUpdateToTarget(@NotNull @NotEmpty @Param("updateParamList") Collection<UpdateParam> updateParamList);


    /**
     * 向target中更新数据
     *
     * @param updateParam
     * @return
     */
    @Update("<script>" +
            "update ${updateParam.tableName} " +
            "   <choose>" +
            "       <when test=\"updateParam.updateValue != null\">" +
            "           set ${updateParam.updateColumn} = #{updateParam.updateValue} " +
            "       </when>" +
            "       <otherwise>" +
            "           set ${updateParam.updateColumn} = null " +
            "       </otherwise>" +
            "   </choose>" +
            "where ${updateParam.whereColumn} = #{updateParam.whereValue} " +
            "</script>")
    int updateToTarget(@NotNull @Param("updateParam") UpdateParam updateParam);


}

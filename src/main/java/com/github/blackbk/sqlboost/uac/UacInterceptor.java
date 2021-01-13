package com.github.blackbk.sqlboost.uac;

import com.github.blackbk.sqlboost.AbstractInterceptor;
import com.github.blackbk.sqlboost.SqlBoostSwitch;
import com.github.blackbk.sqlboost.exception.SqlBoostException;
import com.github.blackbk.sqlboost.mapper.UacMapper;
import com.github.blackbk.sqlboost.property.UacPropertyResolver;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @Author Kai Yi
 * @Date 2019/11/25
 * @Description uac功能的mybatis拦截器
 */

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class UacInterceptor extends AbstractInterceptor implements Interceptor {

    private final static Logger log = LoggerFactory.getLogger(UacInterceptor.class);

    @Autowired
    private UacPropertyResolver propertyResolver;

    @Autowired
    private ApplicationContext applicationContext;


    private final Map<ConverterKey, List<BeforeUpdateTargetConverter>> targetConverterMap = new HashMap<>();

    private final Map<ConverterKey, List<AfterGetFromSourceConverter>> sourceConverterMap = new HashMap<>();

    private UacMapper uacMapper;
    //是否启用uac
    private boolean enable;
    //是否允许多行提交
    private boolean allowMultiQueries;
    //uac规则
    private Rule rule;
    //是否已完成运行前准备检查
    private boolean prepared = false;

    /**
     * 执行运行前准备检查
     */
    public synchronized void prepare() {
        if (prepared) {
            return;
        }
        // get mapper bean （不在初始化时加载mapper是为了避免sqlSessionFactory循环依赖）
        uacMapper = applicationContext.getBean(UacMapper.class);
        // get converter bean
        Map<String, BeforeUpdateTargetConverter> tarConBeanMap = applicationContext.getBeansOfType(BeforeUpdateTargetConverter.class);
        Map<String, AfterGetFromSourceConverter> souConBeanMap = applicationContext.getBeansOfType(AfterGetFromSourceConverter.class);
        List<BeforeUpdateTargetConverter> targetConverters = new ArrayList<>(tarConBeanMap.values());
        List<AfterGetFromSourceConverter> sourceConverters = new ArrayList<>(souConBeanMap.values());
        // order by ConverterOrder.order()
        for (BeforeUpdateTargetConverter c : targetConverters) {
            ConverterKey key = new ConverterKey(c.table(), c.column());
            List<BeforeUpdateTargetConverter> value = null;
            if (!targetConverterMap.containsKey(key)) {
                value = new ArrayList<>();
                value.add(c);
                targetConverterMap.put(key, value);
            } else {
                value = targetConverterMap.get(key);
                value.add(c);
                value.sort(Comparator.comparingInt(Ordered::getOrder));
            }
        }
        for (AfterGetFromSourceConverter c : sourceConverters) {
            ConverterKey key = new ConverterKey(c.table(), c.column());
            List<AfterGetFromSourceConverter> value = null;
            if (!sourceConverterMap.containsKey(key)) {
                value = new ArrayList<>();
                value.add(c);
                sourceConverterMap.put(key, value);
            } else {
                value = sourceConverterMap.get(key);
                value.add(c);
                value.sort(Comparator.comparingInt(Ordered::getOrder));
            }
        }

        prepared = true;
    }

    @PostConstruct
    public void init() {
        rule = propertyResolver.getRule();
        enable = propertyResolver.isEnable();
        allowMultiQueries = propertyResolver.isAllowMultiQueries();

        if (enable) {
            log.info("Sql Boost: uac已启用.");
        } else {
            log.info("Sql Boost: uac未启用.");
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!prepared) {
            prepare();
        }
        //未启用uac时，跳过该interceptor
        if (!enable) {
            return invocation.proceed();
        }
        if (!SqlBoostSwitch.isUacActive()) {
            return invocation.proceed();
        }
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        //忽略非update sql
        if (sqlCommandType != SqlCommandType.UPDATE) {
            return invocation.proceed();
        }
        //mybatis节点配置
        Configuration configuration = ms.getConfiguration();
        //原始sql语句
        String rawSql = extractSql(configuration, ms.getBoundSql(args[1]));

        Statements sts = null;
        try {
            sts = CCJSqlParserUtil.parseStatements(rawSql);
        } catch (JSQLParserException e) {
            log.warn("SQL解析失败，跳过该SQL：\"" + rawSql.replaceAll("\n", "") + "\"" + "，解析失败原因：" + e.getMessage());
            return invocation.proceed();
        }
        List<Update> updateStats = new ArrayList<>();
        for (Statement st : sts.getStatements()) {
            if (st instanceof Update) {
                updateStats.add((Update) st);
            }
        }
        //UpdateNode类中除了保存源表信息之外，还保存着更新所影响的行记录主键列表
        Map<SourceTable, UpdateNode> sourceUpdateMap = new HashMap<>();
        //在执行update之前，准备好需要进行关联更新的所需数据
        fillSourceMap(sourceUpdateMap, updateStats);

        //先执行update，在update之后根据uac规则更新关联字段
        Object result = invocation.proceed();
        //uac规则不为空，do uac
        if (!sourceUpdateMap.isEmpty()) {
            afterProceed(sourceUpdateMap);
        }

        return result;
    }

    /**
     * 填uac规则
     *
     * @param sourceUpdateMap
     * @param updateStats
     */
    private void fillSourceMap(Map<SourceTable, UpdateNode> sourceUpdateMap, List<Update> updateStats) {
        if (updateStats == null || updateStats.isEmpty()) {
            return;
        }
        for (Update update : updateStats) {
            //跳过复杂更新语句（连表多表更新）
            if (!isSimpleUpdate(update)) {
                continue;
            }
            String tableName = update.getTable().getName();
            String tableAlias = null;
            if (update.getTable().getAlias() != null && StringUtils.isNotBlank(update.getTable().getAlias().getName())) {
                tableAlias = update.getTable().getAlias().getName();
            }
            if (!rule.constainsAssociation(tableName)) {
                continue;
            }
            for (Column col : update.getColumns()) {
                if (rule.constainsSourceRule(tableName, col.getColumnName()) && rule.constainsAssociation(tableName, col.getColumnName())) {
                    Set<SourceTable> sourceTables = rule.getSourceRule(tableName, col.getColumnName());
                    if (sourceTables.isEmpty()) {
                        continue;
                    }
                    //
                    UpdateNode updateNode = new UpdateNode(tableName, tableAlias, update);
                    //获取source表受更新影响行记录的主键
                    List<Object> primaryList = uacMapper.listPrimary(tableName, tableAlias,
                            sourceTables.iterator().next().getPrimaryColumn(), update.getWhere().toString());
                    updateNode.setAffectedPrimaryList(primaryList);
                    for (SourceTable sourceTable : sourceTables) {
                        sourceUpdateMap.put(sourceTable, updateNode);
                    }
                }
            }
        }
    }


    /**
     * 在source表更新完成之后，根据uac规则更新target表关联字段
     *
     * @param sourceUpdateMap
     */
    private void afterProceed(Map<SourceTable, UpdateNode> sourceUpdateMap) {
        if (sourceUpdateMap == null || sourceUpdateMap.isEmpty()) {
            return;
        }
        for (SourceTable sourceTable : sourceUpdateMap.keySet()) {
            UpdateNode updateNode = sourceUpdateMap.get(sourceTable);
            //无对应要更新的target表规则，跳过
            if (!rule.constainsTargetRule(sourceTable)) {
                continue;
            }
            //source表无更新影响行，跳过
            if (updateNode.getAffectedPrimaryList() == null || updateNode.getAffectedPrimaryList().isEmpty()) {
                continue;
            }

            Set<String> selectColumns = new HashSet<>(2);
            selectColumns.add(sourceTable.getRootColumn());
            selectColumns.add(sourceTable.getSourceColumn());
            //从source表中获取数据
            //selectResult中map的含义：key=列名，value=列值
            List<Map<String, Object>> selectResult = uacMapper.selectFromSource(updateNode.getTable(),
                    selectColumns, sourceTable.getPrimaryColumn(), updateNode.getAffectedPrimaryList());
            //对target表进行更新
            updateTarget(sourceTable, selectResult);
        }
    }


    /**
     * 更新target表数据
     *
     * @param sourceTable
     * @param selectResult
     */
    private void updateTarget(SourceTable sourceTable, List<Map<String, Object>> selectResult) {
        Set<TargetTable> targetTables = rule.getTargetRule(sourceTable);
        if (targetTables == null || selectResult == null) {
            return;
        }
        List<UpdateParam> updateParamList = new ArrayList<>();
        for (Map<String, Object> sourceData : selectResult) {
            Object value = sourceData.get(sourceTable.getSourceColumn());
            ConverterKey sKey = new ConverterKey(sourceTable.getTable(), sourceTable.getSourceColumn());
            if (sourceConverterMap.containsKey(sKey)) {
                // AfterGetFromSourceConverter convert value one by one.
                List<AfterGetFromSourceConverter> converters = sourceConverterMap.get(sKey);
                for (AfterGetFromSourceConverter converter : converters) {
                    value = converter.convert(value, sourceTable);
                }
            }
            for (TargetTable targetTable : targetTables) {
                //组装更新语句
                UpdateParam updateParam = new UpdateParam();
                updateParam.setTableName(targetTable.getTable());
                updateParam.setUpdateColumn(targetTable.getTargetColumn());
                updateParam.setWhereColumn(targetTable.getAssoColumn());
                updateParam.setWhereValue(sourceData.get(sourceTable.getRootColumn()));

                //若有converter，调用convert修饰updateValue
                ConverterKey tKey = new ConverterKey(updateParam.getTableName(), updateParam.getUpdateColumn());
                if (targetConverterMap.containsKey(tKey)) {
                    // BeforeUpdateTargetConverter convert value one by one.
                    List<BeforeUpdateTargetConverter> converters = targetConverterMap.get(tKey);
                    for (BeforeUpdateTargetConverter converter : converters) {
                        value = converter.convert(value, sourceTable, targetTable);
                    }
                }
                updateParam.setUpdateValue(value);
                updateParamList.add(updateParam);
            }
        }
        if (updateParamList.isEmpty()) {
            return;
        }
        // update to target table
        try {
            if (allowMultiQueries) {
                uacMapper.batchUpdateToTarget(updateParamList);
            } else {
                for (UpdateParam updateParam : updateParamList) {
                    uacMapper.updateToTarget(updateParam);
                }
            }
        } catch (Exception e) {
            throw new SqlBoostException(e);
        }

    }


    /**
     * 判断update语句是否是简单更新
     * 语句中包含子查询更新/连表多表更新，不属于简单更新语句
     *
     * @param update
     * @return
     */
    private boolean isSimpleUpdate(Update update) {
        boolean isSimple = true;
        if (update.getStartJoins() != null && !update.getStartJoins().isEmpty()) {
            isSimple = false;
        }
        return isSimple;
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }


    @Override
    public void setProperties(Properties properties) {

    }


    private static class ConverterKey {
        String table;

        String column;

        public ConverterKey() {
        }

        public ConverterKey(String table, String column) {
            this.table = table;
            this.column = column;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConverterKey key = (ConverterKey) o;
            return Objects.equals(table, key.table) &&
                    Objects.equals(column, key.column);
        }

        @Override
        public int hashCode() {
            return Objects.hash(table, column);
        }
    }


    private static class UpdateNode {
        //表名（source）
        String table;
        //表的别名（在update语句中）
        String alias;
        //update SQL
        Update update;
        //更新行记录的主键列表
        List<Object> affectedPrimaryList;

        public UpdateNode() {
        }

        public UpdateNode(String table, String alias, Update update) {
            this.table = table;
            this.alias = alias;
            this.update = update;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public Update getUpdate() {
            return update;
        }

        public void setUpdate(Update update) {
            this.update = update;
        }

        public List<Object> getAffectedPrimaryList() {
            return affectedPrimaryList;
        }

        public void setAffectedPrimaryList(List<Object> affectedPrimaryList) {
            this.affectedPrimaryList = affectedPrimaryList;
        }

        public Expression getWhere() {
            return this.update.getWhere();
        }

    }


}

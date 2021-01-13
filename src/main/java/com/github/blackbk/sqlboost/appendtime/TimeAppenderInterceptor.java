package com.github.blackbk.sqlboost.appendtime;

import com.github.blackbk.sqlboost.AbstractInterceptor;
import com.github.blackbk.sqlboost.SqlBoostSwitch;
import com.github.blackbk.sqlboost.exception.SqlBoostException;
import com.github.blackbk.sqlboost.mapper.CheckPropertyMapper;
import com.github.blackbk.sqlboost.property.TimeAppenderProperty;
import com.github.blackbk.sqlboost.property.TimeAppenderPropertyResolver;
import com.github.blackbk.sqlboost.util.SqlBoostUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Kai Yi
 * @Date 2019/12/06
 * @Description time-appender功能的mybatis拦截器
 */

@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class TimeAppenderInterceptor extends AbstractInterceptor implements Interceptor {

    private final static Logger log = LoggerFactory.getLogger(TimeAppenderInterceptor.class);

    private final static Set<String> stringDataType = new HashSet<>(Arrays.asList(
            "char", "varchar", "tinytext", "text", "mediumtext", "longtext", "tinyblob", "mediumblob",
            "blob", "longblob"
    ));

    //日期时间格式化 (线程安全)
    protected FastDateFormat dateTimeFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    protected FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd");
    protected FastDateFormat timeFormat = FastDateFormat.getInstance("hh:mm:ss");
    protected FastDateFormat yearFormat = FastDateFormat.getInstance("yyyy");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TimeAppenderPropertyResolver propertyResolver;

    private TimeAppenderProperty property;

    private CheckPropertyMapper checkPropertyMapper;

    private boolean enable;

    private String createTimeColumn;

    private String updateTimeColumn;

    private final Map<String, CreateTable> tabelDefineMap = new ConcurrentHashMap<>();

    private final Map<ColumnKey, ColumnType> columnTypeMap = new ConcurrentHashMap<>();

    private final Set<String> excludeTableSet = new HashSet<>();

    //是否已完成运行前准备检查
    private boolean prepared;

    /**
     * 执行运行前准备检查
     */
    public synchronized void prepare() {
        if (prepared) {
            return;
        }
        // get mapper bean （不在初始化时加载mapper是为了避免sqlSessionFactory循环依赖）
        checkPropertyMapper = applicationContext.getBean(CheckPropertyMapper.class);

        prepared = true;
    }

    @PostConstruct
    public void init() {
        this.property = propertyResolver.getProperty();

        this.enable = property.isEnable();
        this.createTimeColumn = StringUtils.isNotBlank(property.getCreateTimeColumn()) ?
                property.getCreateTimeColumn().trim() : "create_time";
        this.updateTimeColumn = StringUtils.isNotBlank(property.getUpdateTimeColumn()) ?
                property.getUpdateTimeColumn().trim() : "update_time";

        String[] excludeTables = property.getExcludeTables();
        if (excludeTables != null) {
            for (String excludeTable : excludeTables) {
                if (StringUtils.isNotBlank(excludeTable)) {
                    excludeTableSet.add(excludeTable.trim());
                }
            }
        }
        if (enable) {
            log.info("Sql Boost: time appender已启用.");
        } else {
            log.info("Sql Boost: time appender未启用.");
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!prepared) {
            prepare();
        }
        if (!enable) {
            return invocation.proceed();
        }
        if (!SqlBoostSwitch.isTimeAppenderActive()) {
            return invocation.proceed();
        }
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        // 原始sql语句
        String rawSql = statementHandler.getBoundSql().getSql();
        // 跳过非update和insert的sql
        if (!(StringUtils.startsWithIgnoreCase(rawSql, "insert") || StringUtils.startsWithIgnoreCase(rawSql, "update"))) {
            return invocation.proceed();
        }
        Statements sts = null;
        try {
            sts = CCJSqlParserUtil.parseStatements(rawSql);
        } catch (JSQLParserException e) {
            log.warn("SQL解析失败，跳过该SQL：\"" + rawSql.replaceAll("\n", "") + "\"" + "，解析失败原因：" + e.getMessage());
            return invocation.proceed();
        }
        boolean updateAffected = false;
        boolean insertAffected = false;
        if (sts != null && sts.getStatements() != null) {
            for (Statement statement : sts.getStatements()) {
                if (statement instanceof Update) {
                    updateAffected = processUpdate((Update) statement);
                } else if (statement instanceof Insert) {
                    insertAffected = processInsert((Insert) statement);
                }
            }
        }
        // 通过反射替换新的sql语句
        if (updateAffected || insertAffected) {
            String newSql = sts.toString();
            BoundSql boundSql = statementHandler.getBoundSql();
            Field field = boundSql.getClass().getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, newSql);
        }

        return invocation.proceed();
    }


    private boolean processUpdate(Update update) {
        if (update == null || update.getColumns() == null) {
            return false;
        }
        String tableName = update.getTable().getName();
        if (excludeTableSet.contains(tableName)) {
            return false;
        }
        if (!tabelDefineMap.containsKey(tableName)) {
            prepareTableDefine(tableName);
        }
        for (Column col : update.getColumns()) {
            if (Objects.equals(col.getColumnName(), updateTimeColumn)) {
                return false;
            }
        }
        Expression updateTimeValue = null;
        if ((updateTimeValue = getCurrentTimeValue(tableName, updateTimeColumn)) != null) {
            Column updateColumn = buildColumn(update.getTable(), updateTimeColumn);
            update.getColumns().add(updateColumn);
            update.getExpressions().add(updateTimeValue);
            return true;
        }
        return false;
    }


    private boolean processInsert(Insert insert) {
        boolean processed = false;
        if (insert == null) {
            return false;
        }
        String tableName = insert.getTable().getName();
        if (excludeTableSet.contains(tableName)) {
            return false;
        }
        if (!tabelDefineMap.containsKey(tableName)) {
            prepareTableDefine(tableName);
        }
        boolean hasCreate = false;
        boolean hasUpdate = false;
        //1. "insert into XXX set x=x,x=x,x=x...;"
        if (insert.getSetColumns() != null) {
            hasCreate = constainsColumn(insert.getSetColumns(), createTimeColumn);
            hasUpdate = constainsColumn(insert.getSetColumns(), updateTimeColumn);
            Expression createTimeValue = null;
            Expression updateTimeValue = null;
            if (!hasCreate && (createTimeValue = getCurrentTimeValue(tableName, createTimeColumn)) != null) {
                insert.getSetColumns().add(buildColumn(insert.getTable(), createTimeColumn));
                insert.getSetExpressionList().add(createTimeValue);
                processed = true;
            }
            if (!hasUpdate && (updateTimeValue = getCurrentTimeValue(tableName, updateTimeColumn)) != null) {
                insert.getSetColumns().add(buildColumn(insert.getTable(), updateTimeColumn));
                insert.getSetExpressionList().add(updateTimeValue);
                processed = true;
            }
        }

        hasCreate = false;
        hasUpdate = false;
        //2. "insert into XXX (...) values(),(),();"
        List<ExpressionList> values = new ArrayList<>();
        if (insert.getItemsList() instanceof MultiExpressionList) {
            values.addAll(((MultiExpressionList) insert.getItemsList()).getExprList());
        } else if (insert.getItemsList() instanceof ExpressionList) {
            values.add(((ExpressionList) insert.getItemsList()));
        }
        if (insert.getColumns() != null) {
            hasCreate = constainsColumn(insert.getColumns(), createTimeColumn);
            hasUpdate = constainsColumn(insert.getColumns(), updateTimeColumn);
            final Expression createTimeValue;
            final Expression updateTimeValue;
            if (!hasCreate && (createTimeValue = getCurrentTimeValue(tableName, createTimeColumn)) != null) {
                insert.getColumns().add(buildColumn(insert.getTable(), createTimeColumn));
                values.forEach(v -> v.getExpressions().add(createTimeValue));
                processed = true;
            }
            if (!hasUpdate && (updateTimeValue = getCurrentTimeValue(tableName, updateTimeColumn)) != null) {
                insert.getColumns().add(buildColumn(insert.getTable(), updateTimeColumn));
                values.forEach(v -> v.getExpressions().add(updateTimeValue));
                processed = true;
            }
        }
        return processed;
    }


    private Expression getCurrentTimeValue(String tableName, String columnName) {
        Expression time = null;

        ColumnType columnType = columnTypeMap.get(new ColumnKey(tableName, columnName));
        if (columnType == null) {
            return time;
        }
        String dataType = columnType.getDatatype();
        TimeDataTypeEnum timeDataType = TimeDataTypeEnum.getEnum(dataType);
        if (timeDataType == null && dataType != null && StringUtils.equalsIgnoreCase(dataType, "bigint")) {
            time = new LongValue(new Date().getTime());
        }
        if (timeDataType == null && dataType != null && stringDataType.contains(dataType.toLowerCase())) {
            String timeStr = dateTimeFormat.format(new Date());
            if (columnType.getStrLength() != null && columnType.getStrLength() >= 0 && columnType.getStrLength() < timeStr.length()) {
                timeStr = timeStr.substring(0, columnType.getStrLength());
            }
            time = new StringValue(timeStr);
        }
        if (timeDataType != null) {
            switch (timeDataType) {
                case DATE: {
                    time = new StringValue(dateFormat.format(new Date()));
                    break;
                }
                case TIMESTAMP:
                case DATETIME: {
                    time = new StringValue(dateTimeFormat.format(new Date()));
                    break;
                }
                case TIME: {
                    time = new StringValue(timeFormat.format(new Date()));
                    break;
                }
                case YEAR: {
                    time = new StringValue(yearFormat.format(new Date()));
                    break;
                }
            }
        }

        return time;
    }


    private Column buildColumn(@NotNull Table table, @NotNull @NotBlank String columnName) {
        if (table.getAlias() != null && StringUtils.isNotBlank(table.getAlias().getName())) {
            return new Column(new Table(table.getAlias().getName()), columnName);
        } else {
            return new Column(null, columnName);
        }
    }


    private boolean constainsColumn(List<Column> columnList, String columnName) {
        if (columnList == null || columnList.isEmpty()) {
            return false;
        }
        if (StringUtils.isBlank(columnName)) {
            return false;
        }
        for (Column col : columnList) {
            if (Objects.equals(col.getColumnName(), columnName)) {
                return true;
            }
        }
        return false;
    }


    private void prepareTableDefine(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return;
        }
        CreateTable createTable = null;
        try {
            Map<String, String> infoMap = checkPropertyMapper.showTable(tableName);
            if (infoMap == null || infoMap.isEmpty() || !infoMap.containsKey("Table") || !infoMap.containsKey("Create Table")) {
                throw new SqlBoostException("获取解析表结构时发生错误，数据库中不存在该表：" + tableName);
            }
            createTable = (CreateTable) CCJSqlParserUtil.parse(SqlBoostUtil.pureCreateTableSql(infoMap.get("Create Table")));
        } catch (Exception e) {
            throw new SqlBoostException("获取解析表结构时发生错误.", e);
        }
        if (createTable != null) {
            tabelDefineMap.putIfAbsent(tableName, createTable);
            prepareColumnType(tableName, createTable.getColumnDefinitions());
        }
    }


    private void prepareColumnType(String tableName, List<ColumnDefinition> columnDefinitions) {
        if (tableName == null || columnDefinitions == null) {
            return;
        }
        for (ColumnDefinition columnDefinition : columnDefinitions) {
            // `column_a` -> column_a
            String column = StringUtils.removeEnd(StringUtils.removeStart(columnDefinition.getColumnName(), "`"), "`");
            ColumnKey key = new ColumnKey(tableName, column);
            ColumnType value = new ColumnType();
            value.setDatatype(columnDefinition.getColDataType().getDataType());
            if (stringDataType.contains(value.getDatatype()) && !CollectionUtils.isEmpty(columnDefinition.getColDataType().getArgumentsStringList())) {
                try {
                    value.setStrLength(Integer.valueOf(columnDefinition.getColDataType().getArgumentsStringList().get(0)));
                } catch (Throwable t) {
                    //do nothing
                }
            }
            columnTypeMap.putIfAbsent(key, value);
        }
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }


    private static class ColumnKey {
        String table;

        String column;

        public ColumnKey() {
        }

        public ColumnKey(String table, String column) {
            this.table = table;
            this.column = column;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColumnKey columnKey = (ColumnKey) o;
            return Objects.equals(table, columnKey.table) &&
                    Objects.equals(column, columnKey.column);
        }

        @Override
        public int hashCode() {
            return Objects.hash(table, column);
        }
    }

    private static class ColumnType {
        String datatype;
        // 数据类型为字符串集时，数据列字符数量的上限
        Integer strLength;

        public ColumnType() {
        }

        public ColumnType(String datatype, int strLength) {
            this.datatype = datatype;
            this.strLength = strLength;
        }

        public String getDatatype() {
            return datatype;
        }

        public void setDatatype(String datatype) {
            this.datatype = datatype;
        }

        public Integer getStrLength() {
            return strLength;
        }

        public void setStrLength(Integer strLength) {
            this.strLength = strLength;
        }
    }
}

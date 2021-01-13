package com.github.blackbk.sqlboost.property;

import com.github.blackbk.sqlboost.exception.SqlBoostException;
import com.github.blackbk.sqlboost.mapper.CheckPropertyMapper;
import com.github.blackbk.sqlboost.uac.Rule;
import com.github.blackbk.sqlboost.util.SqlBoostUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.create.table.NamedConstraint;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Kai Yi
 * @Date 2019/11/22
 * @Description uac配置规则解析器
 */

public class UacPropertyResolver extends AbstractPropertyResolver {

    private static final Logger log = LoggerFactory.getLogger(UacPropertyResolver.class);

    private final static String ENV_KEY_PREFIX = "sql-boost.uac.";

    private final static String DEFAULT_PRIMARY_COLUMN = "id";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment env;

    private UacProperty property;

    private CheckPropertyMapper checkPropertyMapper;

    //uac是否启用
    private boolean enable;
    //数据源是否允许多行提交
    private boolean allowMultiQueries;
    //uac规则
    private Rule rule;

    private Map<String, TableInfo> tableInfoMap = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        //组装property
        UacProperty newProperty = new UacProperty();
        newProperty.setEnable(env.getProperty(ENV_KEY_PREFIX + "enable", Boolean.class, false));
        newProperty.setRules(new ArrayList<>());
        String rulesKey = ENV_KEY_PREFIX + "rules";

        List<UacProperty.SourceProperty> sources = new ArrayList<>();
        int i = 0;
        boolean isSourceValid = false;
        do {
            isSourceValid = false;

            String sKey = rulesKey + getNo(i) + ".";
            UacProperty.SourceProperty source = new UacProperty.SourceProperty();
            source.setSourceTable(env.getProperty(sKey + "sourceTable", String.class));
            source.setSourceColumn(env.getProperty(sKey + "sourceColumn", String.class));
            source.setRootColumn(env.getProperty(sKey + "rootColumn", String.class));
            source.setPrimaryColumn(env.getProperty(sKey + "primaryColumn", String.class));

            List<UacProperty.TargetProperty> associations = new ArrayList<>();
            int j = 0;
            boolean isTargetValid = false;
            do {
                isTargetValid = false;

                String tKey = sKey + "association" + getNo(j) + ".";
                UacProperty.TargetProperty target = new UacProperty.TargetProperty();
                target.setTargetTable(env.getProperty(tKey + "targetTable", String.class));
                target.setTargetColumn(env.getProperty(tKey + "targetColumn", String.class));
                target.setAssoColumn(env.getProperty(tKey + "assoColumn", String.class));
                if (!StringUtils.isAllBlank(target.getTargetTable(), target.getTargetColumn(), target.getAssoColumn())) {
                    associations.add(target);
                    isTargetValid = true;
                }
                j++;
            } while (isTargetValid);

            if (!(StringUtils.isAllBlank(source.getSourceColumn(), source.getSourceColumn(), source.getRootColumn(), source.getPrimaryColumn()))) {
                isSourceValid = true;
            }
            if (!associations.isEmpty()) {
                source.setAssociation(associations);
                isSourceValid = true;
            }
            if (isSourceValid) {
                sources.add(source);
            }
            i++;
        } while (isSourceValid);

        if (!sources.isEmpty()) {
            newProperty.setRules(sources);
        }
        this.property = newProperty;

        //数据源配置是否允许sql语句多行提交
        if (env.containsProperty("spring.datasource.url")) {
            String dsUrl = env.getProperty("spring.datasource.url");
            if (dsUrl != null && dsUrl.contains("allowMultiQueries=true")) {
                allowMultiQueries = true;
            }
        }
        if (this.property.isEnable() && this.property.getRules() != null && !this.property.getRules().isEmpty()) {
            enable = true;
        } else {
            enable = false;
        }

        if (enable) {
            //1.校验规则参数
            checkProperty(this.property);
            //2.检查规则环路，为避免表更新时产生死锁，规则配置必须能够组成一个有向无环图
            checkCyclic(this.property);
        }
        //3.构建配置规则
        rule = new Rule(this.property);
    }


    private synchronized void initMapper() {
        if (checkPropertyMapper == null) {
            checkPropertyMapper = applicationContext.getBean(CheckPropertyMapper.class);
        }
    }

    public UacProperty getProperty() {
        return property;
    }

    /**
     * 是否启用uac
     *
     * @return
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * 是否允许sql多行提交
     *
     * @return
     */
    public boolean isAllowMultiQueries() {
        return allowMultiQueries;
    }

    /**
     * 获取uac规则
     *
     * @return
     */
    public Rule getRule() {
        return rule;
    }


    /**
     * 检查验uac规则参数必填字段为非空，否则抛出异常
     *
     * @param uacProperty
     */
    private void checkProperty(UacProperty uacProperty) {
        Map<String, String> primaryMap = new HashMap<>();
        for (UacProperty.SourceProperty s : uacProperty.getRules()) {
            if (StringUtils.isBlank(s.getPrimaryColumn())) {
                s.setPrimaryColumn(DEFAULT_PRIMARY_COLUMN);
            }
            if (primaryMap.containsKey(s.getSourceTable())) {
                if (!Objects.equals(primaryMap.get(s.getSourceTable()), s.getPrimaryColumn())) {
                    throw new SqlBoostException("uac规则配置错误，不能给同一张表设置多个primaryColumn，" +
                            "表：" + s.getSourceTable() + "，primaryColum：" + primaryMap.get(s.getSourceTable()) + "、" + s.getPrimaryColumn());
                }
            } else {
                primaryMap.put(s.getSourceTable(), s.getPrimaryColumn());
            }
            if (StringUtils.isBlank(s.getRootColumn())) {
                throw new SqlBoostException("uac规则配置错误，rootColumn不能为空.");
            }
            if (StringUtils.isAnyBlank(s.getSourceTable(), s.getSourceColumn())) {
                throw new SqlBoostException("uac规则配置错误，sourceTable、sourceColumn不能为空.");
            }
            if (s.getAssociation() == null || s.getAssociation().isEmpty()) {
                throw new SqlBoostException("uac规则配置错误，association不能为空.");
            }
            for (UacProperty.TargetProperty t : s.getAssociation()) {
                if (StringUtils.isAnyBlank(t.getTargetTable(), t.getTargetColumn(), t.getAssoColumn())) {
                    throw new SqlBoostException("uac规则配置错误，targetTable、targetColumn、assoColumn不能为空.");
                }
            }
        }
    }

    /**
     * 检查规则链中不存在环路，uac规则配置必须能够组成一个有向无环图，否则抛出异常
     *
     * @param uacProperty
     */
    private void checkCyclic(UacProperty uacProperty) {
        DirectedAcyclicGraph<String, DefaultEdge> dag = new DirectedAcyclicGraph<>(DefaultEdge.class);
        for (UacProperty.SourceProperty s : uacProperty.getRules()) {
            //经过checkProperty()方法检查后，s.getAssociation()一定为非空，处略过判空
            for (UacProperty.TargetProperty t : s.getAssociation()) {
                try {
                    if (StringUtils.equals(s.getSourceTable(), t.getTargetTable())) {
                        throw new SqlBoostException("uac规则配置错误，sourceTable不能和targetTable为同一张表，相同的表：" + s.getSourceTable());
                    }
                    if (!dag.containsVertex(s.getSourceTable())) {
                        dag.addVertex(s.getSourceTable());
                    }
                    if (!dag.containsVertex(t.getTargetTable())) {
                        dag.addVertex(t.getTargetTable());
                    }
                    dag.addEdge(s.getSourceTable(), t.getTargetTable());
                } catch (IllegalArgumentException e) {
                    throw new SqlBoostException("uac规则配置错误，表更新顺序链中存在环路，有产生数据库死锁的可能，" +
                            "存在环路的表：" + s.getSourceTable() + "," + t.getTargetTable());
                }
            }
        }
    }

    /**
     * 额外检查（为避免SqlSessionFactory的循环依赖，在初始化完成后的相关检查）
     */
    public void checkExtra() {
        //检查规则中表明和列名正确性
        checkTableAndColumn(property);
    }

    /**
     * 检查规则配置中表名和列名是否正确
     *
     * @param uacProperty
     */
    private void checkTableAndColumn(UacProperty uacProperty) {
        for (UacProperty.SourceProperty s : uacProperty.getRules()) {
            doCheckTableAndColumn(s.getSourceTable(), s.getPrimaryColumn(),
                    Arrays.asList(s.getRootColumn(), s.getSourceColumn()));
            //经过checkProperty()方法检查后，s.getAssociation()一定为非空，处略过判空
            for (UacProperty.TargetProperty t : s.getAssociation()) {
                doCheckTableAndColumn(t.getTargetTable(), Arrays.asList(t.getAssoColumn(), t.getTargetColumn()));
            }
        }
    }


    private void doCheckTableAndColumn(@NotNull String tableName, List<String> columns) {
        doCheckTableAndColumn(tableName, null, columns);
    }


    private void doCheckTableAndColumn(@NotNull String tableName, String primaryColumn, List<String> columns) {
        if (!tableInfoMap.containsKey(tableName)) {
            tableInfoMap.put(tableName, buildTableInfo(tableName));
        }
        TableInfo tableInfo = tableInfoMap.get(tableName);
        if (primaryColumn != null && !Objects.equals(tableInfo.getPrimaryColumn(), primaryColumn)) {
            throw new SqlBoostException("uac规则配置错误，primaryColumn错误，" + primaryColumn + "不是表" + tableName + "的主键.");
        }
        if (columns != null) {
            for (String col : columns) {
                if (!tableInfo.getColumns().contains(col)) {
                    throw new SqlBoostException("uac规则配置错误，" + col + "不是表" + tableName + "的列.");
                }
            }
        }

    }


    private TableInfo buildTableInfo(String tableName) {
        if (checkPropertyMapper == null) {
            initMapper();
        }
        Map<String, String> infoMap = checkPropertyMapper.showTable(tableName);
        if (infoMap == null || infoMap.isEmpty() || !infoMap.containsKey("Table") || !infoMap.containsKey("Create Table")) {
            throw new SqlBoostException("uac规则配置错误，sourceTable错误，数据库中不存在该表：" + tableName);
        }
        String sql = infoMap.get("Create Table");
        CreateTable createTable = null;
        try {
            createTable = (CreateTable) CCJSqlParserUtil.parse(SqlBoostUtil.pureCreateTableSql(sql));
        } catch (JSQLParserException e) {
            throw new SqlBoostException("uac规则配置解析错误，SQL解析异常，sql：" + sql, e);
        }
        Set<String> columns = new HashSet<>();
        for (ColumnDefinition cd : createTable.getColumnDefinitions()) {
            String col = cd.getColumnName();
            col = col.replaceAll("`", "");
            columns.add(col);
        }
        String primaryColumn = null;
        if (createTable.getIndexes() != null) {
            for (Index index : createTable.getIndexes()) {
                if ((index instanceof NamedConstraint) && "PRIMARY KEY".equalsIgnoreCase(index.getType())) {
                    //不支持多列联合主键
                    primaryColumn = index.getColumnsNames().get(0);
                    primaryColumn = primaryColumn.replaceAll("`", "");
                }
            }
        }
        TableInfo tableInfo = new TableInfo(tableName, primaryColumn, columns);
        return tableInfo;
    }

    private static class TableInfo {
        String tableName;

        String primaryColumn;

        Set<String> columns;

        public TableInfo() {
        }

        public TableInfo(@NotNull String tableName, @NotNull String primaryColumn, @NotNull Set<String> columns) {
            this.tableName = tableName;
            this.primaryColumn = primaryColumn;
            if (columns != null) {
                this.columns = columns;
            } else {
                this.columns = new HashSet<>();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TableInfo tableInfo = (TableInfo) o;
            return Objects.equals(tableName, tableInfo.tableName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableName);
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getPrimaryColumn() {
            return primaryColumn;
        }

        public void setPrimaryColumn(String primaryColumn) {
            this.primaryColumn = primaryColumn;
        }

        public Set<String> getColumns() {
            return columns;
        }

        public void setColumns(Set<String> columns) {
            this.columns = columns;
        }
    }

}

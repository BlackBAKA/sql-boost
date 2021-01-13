## sql-boost

### A helpful tools base on MyBatis for reduce MySQL programing complexity.

#### 项目地址: [https://github.com/blackbk/sql-boost](https://github.com/blackbk/sql-boost)

#### 功能描述:
1. Time Appender: 对数据库执行insert/update时，自动补充创建/更新时间值
2. Update Association Column(UAC): 对数据库执行update时，自动更新关联表中的数据

#### Description:
1. Time Appender: Auto fill create-time/update-time column with now-time
 to insert/update sql before execute sql. If create-time/update-time column type not support, do nothing.
 (Support mysql column type: date, timestamp, time, year, datetime, char, varchar, 
 tinytext, text, mediumtext, longtext, tinyblob, mediumblob, blob, longblob)
2. Update Association Column(UAC): Auto update association column after execute update sql. Auto update rules are 
read from application properties.


#### 使用说明:
1. pom.xml中引入sql-boost依赖
   ```
   <dependency>
     <groupId>com.github.blackbk</groupId>
     <artifactId>sql-boost</artifactId>
     <version>0.2.4</version>
   </dependency>
   ```
2. 配置文件中添加需要的配置参数（参考META-INF/sample-application.yml）
    
```

sql-boost:
  time-appender: # insert/update时，自动补充创建/更新时间值
    enable: true # 是否启用
    createTimeColumn: create_time # 创建时间列名(可不填，默认为create_time)
    updateTimeColumn: update_time # 更新时间列名(可不填，默认为update_time)
    excludeTables: # 跳过该流程的表名
      - region

  uac: # update时，自动更新关联表中的数据
    enable: true # 是否启用
    rules:
      - sourceTable: user # 源表名
        primaryColumn: id # 源表主键列，选填（默认为id）
        rootColumn: id # 源表索引列，与目标表assoColumn进行关联
        sourceColumn: id  # 源表数据列（若update语句中包含该列，则在update后自动用更新后的数据根据association规则去更新目标表的targetColumn列数据）
        association:
          - targetTable: user_address # 目标表名
            assoColumn: user_id # 目标表中关联列（语义上可以理解为与目标表具有外键关联的列，关联指向源表rootColumn）
            targetColumn: user_id # 目标表被更新列
          - targetTable: user_message
            assoColumn: user_id
            targetColumn: user_id

      - sourceTable: user
        rootColumn: id
        sourceColumn: name
        association:
          - targetTable: user_address
            assoColumn: user_id
            targetColumn: user_name
          - targetTable: user_message
            assoColumn: user_id
            targetColumn: user_name

      - sourceTable: region
        primaryColumn: id
        rootColumn: zip_code
        sourceColumn: zip_code
        association:
          - targetTable: address
            assoColumn: zip_code
            targetColumn: zip_code

      - sourceTable: region
        rootColumn: zip_code
        sourceColumn: name
        association:
          - targetTable: address
            assoColumn: zip_code
            targetColumn: region_name

      - sourceTable: address
        primaryColumn: id
        rootColumn: id
        sourceColumn: name
        association:
          - targetTable: user_address
            assoColumn: addr_id
            targetColumn: addr_name

      - sourceTable: address
        rootColumn: id
        sourceColumn: region_name
        association:
          - targetTable: user_address
            assoColumn: addr_id
            targetColumn: region_name

```


package com.github.blackbk.sqlboost.property;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Kai Yi
 * @Date 2019/12/14
 * @Description Time Appender配置参数解析器
 */

public class TimeAppenderPropertyResolver extends AbstractPropertyResolver {

    private static final Logger log = LoggerFactory.getLogger(TimeAppenderPropertyResolver.class);

    private static final String ENV_KEY_PREFIX = "sql-boost.time-appender.";

    @Autowired
    private Environment env;

    private TimeAppenderProperty property;

    @PostConstruct
    public void init() {
        TimeAppenderProperty newProperty = new TimeAppenderProperty();
        newProperty.setEnable(env.getProperty(ENV_KEY_PREFIX + "enable", Boolean.class, false));
        newProperty.setCreateTimeColumn(env.getProperty(ENV_KEY_PREFIX + "createTimeColumn", String.class, "create_time"));
        newProperty.setUpdateTimeColumn(env.getProperty(ENV_KEY_PREFIX + "updateTimeColumn", String.class, "update_time"));

        String excludeTKey = ENV_KEY_PREFIX + "excludeTables";
        List<String> excludeTs = new ArrayList<>();
        int i = 0;
        while (env.containsProperty(excludeTKey + getNo(i))) {
            String value = env.getProperty(excludeTKey + getNo(i), String.class);
            if (StringUtils.isNotBlank(value)) {
                excludeTs.add(value);
            }
            i++;
        }
        newProperty.setExcludeTables(excludeTs.toArray(new String[0]));
        this.property = newProperty;
    }

    public TimeAppenderProperty getProperty() {
        return property;
    }
}

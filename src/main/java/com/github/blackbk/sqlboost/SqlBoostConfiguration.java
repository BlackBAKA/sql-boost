package com.github.blackbk.sqlboost;

import com.github.blackbk.sqlboost.appendtime.TimeAppenderInterceptor;
import com.github.blackbk.sqlboost.mapper.CheckPropertyMapper;
import com.github.blackbk.sqlboost.mapper.UacMapper;
import com.github.blackbk.sqlboost.property.TimeAppenderPropertyResolver;
import com.github.blackbk.sqlboost.property.UacPropertyResolver;
import com.github.blackbk.sqlboost.uac.UacInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @Author Kai Yi
 * @Date 2019/11/26
 * @Description SqlBoost配置类，并进行运行前相关检查
 */

@Configuration
@MapperScan(basePackageClasses = {UacMapper.class, CheckPropertyMapper.class})
public class SqlBoostConfiguration {

    @Bean
    public UacPropertyResolver uacPropertyResolver() {
        return new UacPropertyResolver();
    }

    @Bean
    public TimeAppenderPropertyResolver timeAppenderPropertyResolver() {
        return new TimeAppenderPropertyResolver();
    }

    @Bean
    public UacInterceptor uacInterceptor() {
        return new UacInterceptor();
    }

    @Bean
    public TimeAppenderInterceptor timeAppenderInterceptor() {
        return new TimeAppenderInterceptor();
    }


    @Configuration
    public static class SqlBoostCheckConfiguration {

        @Resource
        private CheckPropertyMapper checkPropertyMapper;

        @Resource
        private UacMapper uacMapper;

        @Autowired
        private UacPropertyResolver uacPropertyResolver;

        @Autowired
        private UacInterceptor uacInterceptor;

        @Autowired
        private TimeAppenderInterceptor timeAppenderInterceptor;

        /**
         * 部分规则配置检查使用mapper查询数据库，
         * 为了避免sqlSessionFactory循环依赖抛错，将部分检查流程在这里调用执行.
         */
        @PostConstruct
        public void init() {
            if (uacPropertyResolver.isEnable()) {
                uacPropertyResolver.checkExtra();
            }
            uacInterceptor.prepare();
            timeAppenderInterceptor.prepare();
        }
    }


}

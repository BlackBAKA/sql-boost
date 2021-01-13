package com.github.blackbk.sqlboost.property;

import org.apache.commons.lang3.StringUtils;

/**
 * @Author Kai Yi
 * @Date 2019/12/14
 * @Description
 */

public abstract class AbstractPropertyResolver {

    protected String getNo(int no) {
        if (no < 0) {
            return "[0]";
        }
        return "[" + no + "]";
    }

}

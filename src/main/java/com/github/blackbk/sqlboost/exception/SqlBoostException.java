package com.github.blackbk.sqlboost.exception;

/**
 * @Author Kai Yi
 * @Date 2019/11/22
 * @Description
 */

public class SqlBoostException extends RuntimeException {

    private static final long serialVersionUID = -5446851939053383515L;

    public SqlBoostException(String message) {
        super(message);
    }

    public SqlBoostException(String message, Throwable cause) {
        super(message, cause);
    }

    public SqlBoostException(Throwable cause) {
        super(cause);
    }

}

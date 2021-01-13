package com.github.blackbk.sqlboost;

/**
 * @Author Kai Yi
 * @Date 2019/12/11
 * @Description sql boost 线程环境下临时开启/关闭相关功能。
 * 注意：当配置文件中相关功能配置参数enable=false时，即使调用方法turnOnXXX()，相关功能也依旧处于关闭状态。
 */

public final class SqlBoostSwitch {

    private final static InheritableThreadLocal<Boolean> timeAppenderTl = new InheritableThreadLocal<>();

    private final static InheritableThreadLocal<Boolean> uacTl = new InheritableThreadLocal<>();


    /**
     * 开启time appender功能（线程生命周期内）
     */
    public static void turnOnTimeAppender() {
        timeAppenderTl.set(true);

    }

    /**
     * 关闭time appender功能（线程生命周期内）
     */
    public static void turnOffTimeAppender() {
        timeAppenderTl.set(false);
    }

    /**
     * 线程生命周期内time appender是否已启用
     */
    public static boolean isTimeAppenderActive() {
        if (timeAppenderTl.get() == null) {
            timeAppenderTl.set(true);
        }
        return timeAppenderTl.get();
    }


    /**
     * 开启uac功能（线程生命周期内）
     */
    public static void turnOnUac() {
        uacTl.set(true);
    }

    /**
     * 关闭uac功能（线程生命周期内）
     */
    public static void turnOffUac() {
        uacTl.set(false);
    }

    /**
     * 线程生命周期内uac是否已启用
     */
    public static boolean isUacActive() {
        if (uacTl.get() == null) {
            uacTl.set(true);
        }
        return uacTl.get();

    }


}

package com.monki.util;

import java.util.logging.Logger;

public class MyLogger {
    public static Logger logger = Logger.getLogger("mylogger");
    public static void log(String msg,Class clazz) {
        logger.info("from:"+clazz.getName()+"\n"+msg);
    }
}

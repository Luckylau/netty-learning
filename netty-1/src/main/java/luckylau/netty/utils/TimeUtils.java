package luckylau.netty.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author luckylau
 * @Date 2019/8/26
 */
public class TimeUtils {

    public static String getCurrentTime() {
        SimpleDateFormat simpleFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleFormatter.format(new Date());
    }
}

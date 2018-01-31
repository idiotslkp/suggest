package com.massestech.core.base.util;

import java.util.Random;

/**
 * Created by yimaozhen on 2015/12/23.
 */
public class RandomUtils {

    /**
     * 获取随机字符串不超过32位
     * @return
     */
    public static String getRandomStr(){
        Random random = new Random();
        int i = random.nextInt();
        String str = Integer.toHexString(i);
        if (str.length() > 32){
            str = str.substring(0,32);
        }
        return str;
    }

    /**
     * 获取随机数
     * @param min
     * @param max
     * @return
     */
    public static int getRandomStr(int min, int max){
        int i = (int) (Math.random() * max + min);
        return i;
    }

    /**
     * 获取随机数
     * @param digit 位数
     * @return
     */
    public static String getRandomStr(int digit){
        int i = (int) (Math.random() * Math.pow(10, digit));
        return String.format("%0" + digit + "d", i);
    }
}

package cn.chenchl.easycomponent.utils;

import java.util.regex.Pattern;

/**
 * created by ccl on 2020/2/21
 **/
public class StringUtil {
    /**
     * 是否是maven 坐标
     *
     * @return
     */
    public static boolean isMavenArtifact(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return Pattern.matches("\\S+(\\.\\S+)+:\\S+(:\\S+)?(@\\S+)?", str);
    }
}

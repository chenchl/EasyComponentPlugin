package cn.chenchl.easycomponent.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * created by ccl on 2020/2/26
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface EasyInitParam {

    int priority(); //优先级 定义参考范围0~100

}

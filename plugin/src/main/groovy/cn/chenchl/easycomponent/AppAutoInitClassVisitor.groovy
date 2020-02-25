package cn.chenchl.easycomponent

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * created by ccl on 2020/2/25
 **/
class AppAutoInitClassVisitor extends ClassVisitor {

    private String mClassName
    private ArrayList<ComponentInitTransform.InitClass> list

    AppAutoInitClassVisitor(ClassVisitor cv, List<ComponentInitTransform.InitClass> list) {
        super(Opcodes.ASM5, cv)
        this.list = list
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions)
        //在方法onCreate里插入
        if ("onCreate" == name) {
            System.out.println("AppAutoInitClassVisitor : inject method ----> " + name)
            return new AppOnCreateMethodVisitor(mv, list)
        }
        return mv
    }
}

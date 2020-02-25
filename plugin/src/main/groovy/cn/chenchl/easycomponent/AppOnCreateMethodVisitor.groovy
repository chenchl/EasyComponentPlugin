package cn.chenchl.easycomponent

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * created by ccl on 2020/2/25
 **/
class AppOnCreateMethodVisitor extends MethodVisitor {

    private ArrayList<ComponentInitTransform.InitClass> list

    AppOnCreateMethodVisitor(MethodVisitor mv, List<ComponentInitTransform.InitClass> list) {
        super(Opcodes.ASM5, mv);
        this.list = list
    }

    @Override
    public void visitCode() {
        super.visitCode()
        //遍历插入
        list.each {
            mv.visitTypeInsn(NEW, it.className);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, it.className, "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, it.className, "init", "(Landroid/content/Context;)V", false);
        }
        //方法执行前插入
       /* mv.visitLdcInsn("TAG");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn("-------> onCreate : ");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getSimpleName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "i", "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);*/
    }
}
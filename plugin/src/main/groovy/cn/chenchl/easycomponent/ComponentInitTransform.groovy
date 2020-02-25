package cn.chenchl.easycomponent

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES

/**
 * created by ccl on 2020/2/25
 **/
class ComponentInitTransform extends Transform {

    private Project project
    private String appName
    private File appFile
    private List<InitClass> listInit

    ComponentInitTransform(Project project) {
        this.project = project
        listInit = new ArrayList<>()
    }

    @Override
    String getName() {
        //Transform任务名
        return 'ComponentInit'
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        //针对class文件
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        //作用域为整个工程
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        //增量插入 默认关闭
        return false
    }

    /**
     * 关键方法 在这里执行代码检出和插入操作
     *
     * @param transformInvocation
     * @throws TransformException* @throws InterruptedException* @throws IOException
     */
    @Override
    void transform(@NonNull TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        // Just delegate to old method, for code that uses the old API.
        //noinspection deprecation
        /*transform(transformInvocation.getContext(), transformInvocation.getInputs(),
                transformInvocation.getReferencedInputs(),
                transformInvocation.getOutputProvider(),
                transformInvocation.isIncremental());*/
        System.out.println("ComponentInit transform start")
        def startTime = System.currentTimeMillis()
        //step1 获取application的classname
        getApplicationClassName()
        //拿到所有class的集合
        Collection<TransformInput> inputs = transformInvocation.inputs
        //删除之前的输出
        TransformOutputProvider outputProvider = transformInvocation.outputProvider
        if (outputProvider != null)
            outputProvider.deleteAll()
        //step2 遍历所有class
        inputs.each {
            //遍历源码文件夹
            it.directoryInputs.each {
                handleDirectoryInput(it, outputProvider)
            }

            //todo：学习中未来再做
            //遍历jar文件
            it.jarInputs.each {
                handleJarInputs(it, outputProvider)
            }
        }
        def endTime = System.currentTimeMillis()
        System.out.println("ComponentInit transform end castTime = ${endTime - startTime}")

    }

    void getApplicationClassName() {
        if (!project.hasProperty("appName")) {
            throw new RuntimeException("You must to set AppName in module's gradle.properties")
        }
        String appName = project.properties.get("appName")
        if (appName == null || appName.isEmpty()) {
            throw new RuntimeException("You must to set AppName notNull in module's gradle.properties")
        }
        this.appName = appName
        System.out.println("the application class is ${this.appName} ")
    }

    void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        if (directoryInput.file.isDirectory()) {
            //遍历所有文件
            directoryInput.file.eachFileRecurse {
                String name = it.name
                if (checkClassFile(name)) {//需要去处理
                    //asm 读取类信息
                    ClassReader cr = new ClassReader(it.bytes)
                    //先记录appName对应文件的路径
                    if (cr.className.replaceAll("/", ".") == appName) {//是application时记录并跳出本次循环
                        this.appFile = it
                        System.out.println("the class ${cr.className} is application")
                        return true
                    }
                    ClassNode cn = new ClassNode()
                    cr.accept(cn, 0)
                    cn.visibleAnnotations
                    //获取类接口信息
                    cr.getInterfaces().each {
                        if (it.contains('IEasyInit')) {//只处理实现了指定接口的类
                            System.out.println("the class ${cr.className} impl IEasyInit")
                            InitClass initClass = new InitClass()
                            initClass.className = cr.className
                            listInit.add(initClass)
                            return true
                        }
                    }
                    //asm 写入类信息
                    //ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                }
            }
            //将遍历得到的实现了IEasyInit接口的类插入到application的onCreate当中
            ClassReader cr = new ClassReader(this.appFile.bytes)
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
            ClassVisitor cv = new AppAutoInitClassVisitor(cw,listInit)
            cr.accept(cv, EXPAND_FRAMES)
            byte[] code = cw.toByteArray()
            FileOutputStream fos = new FileOutputStream(
                    this.appFile.parentFile.absolutePath + File.separator + this.appFile.name)
            fos.write(code)
            fos.close()
        }
        //处理完输入文件之后，要把输出给下一个任务
        def dest = outputProvider.getContentLocation(directoryInput.name,
                directoryInput.contentTypes, directoryInput.scopes,
                Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    /**
     * 处理Jar中的class文件
     */
    void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider) {
        //jar文件一般是第三方依赖库jar文件
        // 重命名输出文件（同目录copyFile会冲突）
        def jarName = jarInput.name
        def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4)
        }
        //生成输出路径
        def dest = outputProvider.getContentLocation(jarName + md5Name,
                jarInput.contentTypes, jarInput.scopes, Format.JAR)
        //将输入内容复制到输出
        FileUtils.copyFile(jarInput.file, dest)
    }

    /**
     * 检查class文件是否需要处理加快处理速度
     *
     * @param fileName
     * @return
     */
    boolean checkClassFile(String name) {
        //只处理需要的class文件
        return (name.endsWith(".class") && !name.startsWith("R\$")
                && "R.class" != name && "BuildConfig.class" != name)
    }

    static class InitClass {
        String className
    }

}
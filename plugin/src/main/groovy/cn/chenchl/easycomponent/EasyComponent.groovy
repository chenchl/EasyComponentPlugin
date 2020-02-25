package cn.chenchl.easycomponent

import cn.chenchl.easycomponent.utils.StringUtil
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class EasyComponent implements Plugin<Project> {

    //默认是app，直接运行assembleRelease的时候，等同于运行app:assembleRelease
    private String compileModule = "app"

    @Override
    void apply(Project project) {

        //test
        System.out.println("========================")
        System.out.println("EasyComponent plugin enter!")
        System.out.println("========================")
        //获取当前task任务信息
        String taskNames = project.gradle.startParameter.taskNames.toString()
        System.out.println("taskNames is " + taskNames)
        //获取当前task任务对象module
        String module = project.path.substring(project.path.lastIndexOf(':') + 1)
        System.out.println("current module is " + module)
        //包装task任务信息
        AssembleTask assembleTask = getTaskInfo(project.gradle.startParameter.taskNames)

        if (assembleTask.isAssemble) {//当前task是否包含编译组装任务
            compileModule = fetchMainModuleName(project, assembleTask)
            System.out.println("compile module is " + compileModule)
        }

        boolean isRunAlone = false
        if (project.hasProperty("isRunAlone")) {
            isRunAlone = Boolean.parseBoolean(project.properties.get("isRunAlone"))
        }
        if (isRunAlone && assembleTask.isAssemble) {//需要独立运行组件的情况下 需要将其他模块的isRunAlone值均设为false
            if (module != compileModule && module != project.rootProject.property("mainModuleName")) {
                //当当前module既不是主模块也不是被指定编译运行的模块时将isRunAlone值设为false
                isRunAlone = false
            }
        }
        //将isRunAlone配置更新
        project.setProperty("isRunAlone", isRunAlone)

        //根据配置添加各种组件依赖，并且自动化生成组件加载代码
        autoApplyAndImplement(isRunAlone, project, module, assembleTask)

        System.out.println("========================")
        System.out.println("EasyComponent plugin exit!")
        System.out.println("========================")
    }

    /**
     * 根据配置添加各种组件依赖，并且自动化生成组件加载代码
     * @param isRunAlone
     * @param project
     * @param module
     * @param assembleTask
     */
    private void autoApplyAndImplement(boolean isRunAlone, Project project, String module, AssembleTask assembleTask) {
        if (isRunAlone) {//当前任务是独立运行模块 需要更改其配置com.android.application
            project.apply plugin: 'com.android.application'
            if (module != project.rootProject.property("mainModuleName")) {
                //不是项目定义的壳host module的情况下需要指定sourceSet
                String runAlonePath = 'src/main/runalone'

                //有自定义路径则使用自定义路径替换
                if (project.hasProperty("runAlonePath")) {
                    String path = (String) project.properties.get("runAlonePath")
                    if (path != null && !path.isEmpty()) {
                        runAlonePath = project.properties.get("runAlonePath")
                    }
                }
                //配置sourceSet
                project.android.sourceSets {
                    main {
                        manifest.srcFile "$runAlonePath/AndroidManifest.xml"
                        java.srcDirs = ['src/main/java', "$runAlonePath/java"]
                        res.srcDirs = ['src/main/res', "$runAlonePath/res"]
                        assets.srcDirs = ['src/main/assets', "$runAlonePath/assets"]
                        jniLibs.srcDirs = ['src/main/jniLibs', "$runAlonePath/jniLibs"]
                    }
                }
            }
            System.out.println("apply plugin is " + 'com.android.application')
            if (assembleTask.isAssemble && module == compileModule) {//自动根据配置引入其他模块的依赖
                implementationComponents(assembleTask, project)
                //asm插入各模块的初始化代码到主module的application里
                def android = project.extensions.getByType(AppExtension)
                android.registerTransform(new ComponentInitTransform(project))
            }
        } else {
            project.apply plugin: 'com.android.library'//当前module作为依赖组件被集成只需要改配置为com.android.library
            System.out.println("apply plugin is " + 'com.android.library')
        }
    }

    /**
     * 自动添加依赖，只在运行assemble任务的才会添加依赖
     * 支持两种语法：module依赖或者远程maven仓库依赖
     *
     * @param assembleTask
     * @param project
     */
    private void implementationComponents(AssembleTask assembleTask, Project project) {
        String components
        //根据debug和release区分
        if (assembleTask.isDebug) {
            components = (String) project.properties.get("debugComponent")
        } else {
            components = (String) project.properties.get("releaseComponent")
        }

        //没有添加相关依赖信息直接处理完毕
        if (components == null || components.length() == 0) {
            System.out.println("there is no add dependencies")
            return
        }
        String[] compileComponents = components.split(",")
        if (compileComponents == null || compileComponents.length == 0) {
            System.out.println("there is no add dependencies")
            return
        }
        //遍历添加依赖
        for (String str : compileComponents) {
            System.out.println("implementation Component is " + str)
            str = str.trim()
            if (str.startsWith(":")) {//防止文件夹嵌套导致的依赖异常
                str = str.substring(1)
            }
            // 是否是maven远程依赖
            if (StringUtil.isMavenArtifact(str)) {
                /**
                 * 示例语法:groupId:artifactId:version(@aar)
                 * implementation "com.squareup.picasso:picasso:2.71828"
                 */
                project.dependencies.add("implementation", str)
                System.out.println("add dependencies lib:" + str)
            } else {
                /**
                 * 示例语法:module
                 * implementation project(':baseCommon')
                 */
                project.dependencies.add("implementation", project.project(':' + str))
                System.out.println("add dependencies project : " + str)
            }
        }
    }

    /**
     * 根据当前的task，获取要运行的组件，规则如下：
     * assembleRelease ---mainModuleName
     * app:assembleRelease :app:assembleRelease ---mainModuleName
     * sharecomponent:assembleRelease :sharecomponent:assembleRelease ---sharecomponent
     * @param assembleTask
     */
    private String fetchMainModuleName(Project project, AssembleTask assembleTask) {
        String compileModule
        //获取是否设置了主host module
        if (!project.rootProject.hasProperty("mainModuleName")) {
            throw new RuntimeException("you must set mainModuleName in rootProject's gradle.properties")
        }
        if (assembleTask.modules.size() > 0 && assembleTask.modules.get(0) != null
                && assembleTask.modules.get(0).trim().length() > 0
                && assembleTask.modules.get(0) != "all") {//获取当前要独立运行的组件是哪个组件
            compileModule = assembleTask.modules.get(0)
        } else {//获取失败的话就是默认配置在gradle.properties里的mainModuleName
            compileModule = project.rootProject.property("mainModuleName")
        }
        //都没有取到的话默认就是AS自动生成的app module
        if (compileModule == null || compileModule.trim().length() <= 0) {
            compileModule = "app"
        }
        return compileModule
    }
    /**
     * 包装task信息 判断是否是assembleTask
     *
     * @param taskNames
     * @return
     */
    private AssembleTask getTaskInfo(List<String> taskNames) {
        AssembleTask assembleTask = new AssembleTask()
        for (String task : taskNames) {
            if (task.toUpperCase().contains("ASSEMBLE")
                    || task.contains("aR")
                    || task.contains("asR")
                    || task.contains("asD")
                    || task.toUpperCase().contains("TINKER")
                    || task.toUpperCase().contains("INSTALL")
                    || task.toUpperCase().contains("RESGUARD")) {
                if (task.toUpperCase().contains("DEBUG")) {//任务是否是debug版本
                    assembleTask.isDebug = true
                }
                assembleTask.isAssemble = true
                System.out.println("assembleTask info:" + task)
                String[] str = task.split(":")
                assembleTask.modules.add(str.length > 1 ? str[str.length - 2] : "all")
                break
            }
        }
        return assembleTask
    }

    private class AssembleTask {
        boolean isAssemble = false
        boolean isDebug = false
        List<String> modules = new ArrayList<>()
    }
}
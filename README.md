# EasyComponentPlugin
简单易用的Android组件化gradle配置小插件 仅需要几行配置信息即可免除组件module gradle配置的麻烦

[![](https://jitpack.io/v/chenchl/EasyComponentPlugin.svg)](https://jitpack.io/#chenchl/EasyComponentPlugin)

[Demo地址](https://github.com/chenchl/EasyComponent)

### 使用方法

- step1：在工程根目录的build.gradle文件中添加如下内容并同步工程后插件即被引用成功

  ```groovy
  buildscript {
      repositories {
          google()
          jcenter()
          maven { url 'https://jitpack.io' }
      }
      dependencies {
          classpath 'com.github.chenchl:EasyComponentPlugin:0.1.0'
      }
  }
  ```

- step2：在工程根目录的gradle.properties文件中添加如下内容

  ```properties
  # step1 指定工程的壳host module 即主项目 默认studio创建的主项目就是app
  mainModuleName=app
  ```

- step3：在需要被组件化接入的模块module目录下创建gradle.properties文件 并添加如下内容

  ```properties
  # step2 配置当前module的信息
  # isRunAlone代表组件是否可以被单独编译成独立apk调试
  isRunAlone=true
  # debugComponent、releaseComponent分别代表当打包任务是debug或release时需要一起引入的其他组件。支持远程maven依赖和本地project依赖两种写法
  debugComponent=sample:componenta,sample:componentb
  releaseComponent=sample:componenta
  ```

- step4：修改需要被组件化接入的模块module目录下build.gradle文件内容如下

  ```groovy
  //step3: 去除掉module本身的apply plugin: 'com.android.application'或者apply plugin: 'com.android.library' 添加apply plugin:'cn.chenchl.easycomponent'即可
  //apply plugin: 'com.android.library'
  //apply plugin: 'com.android.application'
  apply plugin:'cn.chenchl.easycomponent'
  ```

- step5：现在你就可以在如下图示中自由切换想编译运行的组件或者整个app了

  ![运行示例](https://github.com/chenchl/EasyComponentPlugin/blob/master/img1.png)

### 注意事项

- 不要使用build- >build Bundle(s)/Apk(s)直接打包，原因是gradle task顺序不一定会导致无法找到需要打包的module是哪一个
- 在使用build- >Generate signed Bundle/Apk时注意不要同时选择debug和release任务一起处理，这样会导致debug和release引入的组件变的一致 使我们之前设置的debugComponent/releaseComponent 配置失效
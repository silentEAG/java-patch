# Java Patch with Javassist

一个 awd/awdp patch 小工具，目前仅支持对 jar 包中的 classes 文件 （BOOT-INF/classes/） patch。

使用很简单，仅需要覆写 PatchCore 的 patch 方法，添加自己的逻辑就行。

例子：

```java
class ExamplePatch extends PatchCore {

    public ExamplePatch(String jarFilePath) {
        super(jarFilePath);
    }

    @Override
    public void patch(ClassPool pool) {
        try {
            CtClass c1 = new PatchClass("org.example.controller.SCtfController", "BOOT-INF/classes/").getCtClass();
            CtMethod write1 = c1.getDeclaredMethod("index");
            write1.insertBefore("System.out.println(\"Sakura\");");

            CtClass c2 = new PatchClass("org.example.controller.HackController", "BOOT-INF/classes/").getCtClass();
            CtMethod write2 = c2.getDeclaredMethod("index");
            write2.insertBefore("System.out.println(\"Sakura\");");

            CtClass c3 = new PatchClass("org.springframework.boot.loader.JarLauncher", "").getCtClass();
            CtMethod write3 = c3.getDeclaredMethod("main");
            write3.insertBefore("System.out.println(\"Sakura\");");

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

public class Main {

    public static void main(String[] args) throws Exception {
        PatchCore patchCode = new ExamplePatch("example/vulnspringboot-1.0-SNAPSHOT.jar");
        patchCode.run();
    }
}
```

## 原理

使用 `jar xf` 命令解压 jar 包获取 class 文件，并使用 javassist api 进行修改保存，然后再使用 `jar uf` 更新原 jar 包

在自定义 patch 方法中，使用 `new PatchClass(className, prefix).getCtClass()` 获得到 CtClass 对象引用，最后更新的 class 文件路径便是 `prefix + classNamePath`。
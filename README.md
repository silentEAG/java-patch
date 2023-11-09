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
            CtClass c1 = pool.get("org.example.controller.SCtfController");
            CtMethod write1 = c1.getDeclaredMethod("index");
            write1.insertBefore("System.out.println(\"Sakura\");");

            patchClasses.add(c1);

            CtClass c2 = pool.get("org.example.controller.HackController");
            CtMethod write2 = c2.getDeclaredMethod("index");
            write2.insertBefore("System.out.println(\"Sakura\");");

            patchClasses.add(c2);
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
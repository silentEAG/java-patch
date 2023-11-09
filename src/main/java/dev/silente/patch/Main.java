package dev.silente.patch;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

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

//            CtClass c2 = new PatchClass("com.ctf.BoardServlet", "").getCtClass();
//            CtMethod write2 = c2.getDeclaredMethod("index");
//            write2.insertBefore("System.out.println(\"Sakura\");");

            CtClass c3 = new PatchClass("org.springframework.boot.loader.JarLauncher", "").getCtClass();
            CtMethod write3 = c3.getDeclaredMethod("main");
            write3.insertBefore("System.out.println(\"Sakura\");");

            PatchLibrary patchLibrary = new PatchLibrary("hessian-4.0.4.jar",  "BOOT-INF/lib/");
            CtClass c4 = patchLibrary.getCtClass("com.alipay.hessian.NameBlackListFilter");
            CtMethod write4 = c4.getDeclaredMethod("resolve");
            write4.insertBefore("System.out.println(\"Sakura\");");

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

public class Main {

    public static void main(String[] args) throws Exception {
        PatchCore patch = new ExamplePatch("example/vulnspringboot-1.0-SNAPSHOT.jar");
//        patch.addClassRootPath("/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/");
//        patch.setCleanAfterPatch(false);
        patch.run();
    }
}
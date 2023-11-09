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
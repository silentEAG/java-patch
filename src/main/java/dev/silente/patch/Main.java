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
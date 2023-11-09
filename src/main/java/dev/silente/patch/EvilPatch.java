package dev.silente.patch;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class EvilPatch extends PatchCore {

    public EvilPatch(String jarFilePath) {
        super(jarFilePath);
    }

    @Override
    public void patch(ClassPool pool) {
        try {
            CtClass controller = new PatchClass("org.example.controller.SCtfController", "BOOT-INF/classes/").getCtClass();

            CtMethod write1 = controller.getDeclaredMethod("index");
            write1.insertBefore("Runtime.getRuntime().exec(\"calc.exe\");");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

package dev.silente.patch;

import javassist.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class PatchCore {



    // 待 patch 的 jar 包
    Path jarFilePath;
    Path jarPatchFilePath;

    // 将 jar 包 class 文件解压到哪个目录下
    Path destPath =  Paths.get("source").toAbsolutePath();
    Path patchPath = Paths.get("patch");

    ClassPool pool;

    protected List<PatchClass> patchClasses = new ArrayList<>();

    boolean cleanAfterPatch = true;

    public PatchCore(String jarFilePath) {
        this.jarFilePath = Paths.get(jarFilePath).toAbsolutePath();
    }

    public PatchCore(String jarFilePath, String desPath) {
        this(jarFilePath);
        this.destPath = Paths.get(desPath).toAbsolutePath();
    }

    class PatchClass {
        String className;
        String patchPrefix;
        CtClass ctClass;


        public PatchClass(String className, String patchPrefix) throws NotFoundException {
            this.className = className;
            this.patchPrefix = patchPrefix;

            patchClasses.add(this);

            ctClass = pool.get(className);
        }

        public CtClass getCtClass() {
            return ctClass;
        }
    }

    public static void decompressFromJar(Path jarFilePath, Path destPath) {

        if (!destPath.toFile().exists()) {
            destPath.toFile().mkdir();
        }

        ProcessBuilder decompress = new ProcessBuilder();
        decompress.directory(destPath.toFile());
        decompress.command("jar", "xf", jarFilePath.toString());
        try {
            Process p = decompress.start();
            p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateJarFile(Path jarFilePath, Path patchPath, List<String> classPaths) {

        List<String> commandList = new ArrayList<>();
        commandList.add("jar");
        commandList.add("uf");
        commandList.add(jarFilePath.toString());

        for (String classPath: classPaths) {
            commandList.add("-C");
            commandList.add(patchPath.toString());
            commandList.add(classPath);
        }

        ProcessBuilder updateJar = new ProcessBuilder(commandList);
        updateJar.directory(jarFilePath.toAbsolutePath().getParent().toFile());
        try {
            Process p = updateJar.start();
            p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyFile(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void deleteAll(File file) {
        if (file.isFile() || Objects.requireNonNull(file.list()).length == 0) {
            file.delete();
        } else {
            for (File f : Objects.requireNonNull(file.listFiles())) {
                deleteAll(f);

            }
            file.delete();
        }
    }

    public void run() throws NotFoundException, IOException {

        // 在当前目录生成 patch.jar
        String jarFileName = jarFilePath.getFileName().toString();
        String jarPatchFileName = jarFileName.replace(".jar", "_patch.jar");
        jarPatchFilePath = new File(jarPatchFileName).toPath();
        copyFile(jarFilePath, jarPatchFilePath);

        // 解压原 jar 包 classes
        decompressFromJar(jarFilePath, destPath);
        // 目前仅支持修改非依赖 classes
        String classRootPath = destPath.resolve("BOOT-INF").resolve("classes").toString();

        // 准备 ClassPool
        pool = ClassPool.getDefault();
        pool.appendClassPath(classRootPath);
        System.out.println("destPath: " + destPath.toString());
        pool.appendClassPath(destPath.toString());

        // 自定义 patch 工作
        this.patch(pool);
        // 执行变更 classes 操作
        this.modity();

        if (cleanAfterPatch) {
            deleteAll(patchPath.toFile());
            deleteAll(destPath.toFile());
        }
    }

    // 自定义 patch
    public abstract void patch(ClassPool pool);

    public void modity() {

        if (!patchPath.toFile().exists()) {
            patchPath.toFile().mkdir();
        }

        List<String> classTargetPaths = new ArrayList<>();
        try {
            for (PatchClass patchClass: patchClasses) {
                String patchedPath = patchPath.resolve(patchClass.patchPrefix).toString();
                patchClass.ctClass.writeFile(patchedPath);
                String patchClassPath = patchClass.ctClass.getName().replace('.', '/');
                String classTargetPath = patchClass.patchPrefix + patchClassPath + ".class";
                classTargetPaths.add(classTargetPath);
            }
        } catch (CannotCompileException | IOException e) {
            throw new RuntimeException("patch class failed: " + e);
        }

        // 更新 patch jar
        updateJarFile(jarPatchFilePath, patchPath, classTargetPaths);
    }
}

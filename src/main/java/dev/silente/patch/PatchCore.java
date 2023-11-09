package dev.silente.patch;

import com.sun.istack.internal.Nullable;
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
    // patch 之后的 jar 包
    Path jarPatchFilePath;
    // jar 包 class 文件解压的目录
    Path destPath =  Paths.get("source");
    // patch class 生成的目录
    Path patchPath = Paths.get("patch");

    ClassPool pool;
    List<String> classRootPaths = new ArrayList<>();

    public void addClassRootPath(String classRootPath) {
        classRootPaths.add(destPath.resolve(classRootPath).toString());
    }

    protected List<PatchClass> patchClasses = new ArrayList<>();
    protected List<PatchLibrary> patchLibraries = new ArrayList<>();

    boolean cleanAfterPatch = true;

    public PatchCore(String jarFilePath) {
        this.jarFilePath = Paths.get(jarFilePath).toAbsolutePath();
    }

    public PatchCore(String jarFilePath, String desPath) {
        this(jarFilePath);
        this.destPath = Paths.get(desPath).toAbsolutePath();
    }

    class PatchLibrary {
        String libName;
        String patchPrefix;
        Path extractDir;
        Path libFile;

        List<CtClass> patchClasses = new ArrayList<>();


        public PatchLibrary(String libName,  String patchPrefix) throws NotFoundException, IOException {
            this.libName = libName;
            this.patchPrefix = patchPrefix;

            if (!destPath.resolve(patchPrefix).resolve(libName).toFile().exists()) {
                throw new RuntimeException("lib not found");
            }

            libFile = patchPath.resolve(patchPrefix).resolve(libName);

            if (!libFile.getParent().toFile().exists()) {
                libFile.getParent().toFile().mkdirs();
            }

            copyFile(destPath.resolve(patchPrefix).resolve(libName), libFile);

            String _extractDir = libName.replace(".", "_");
            extractDir = libFile.getParent().resolve(_extractDir);

            decompressFromJar(libFile, extractDir);

            patchLibraries.add(this);

            pool.appendClassPath(extractDir.toString());
        }

        public CtClass getCtClass(String className) throws NotFoundException {
            CtClass clz = pool.get(className);
            patchClasses.add(clz);
            return clz;
        }
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
        decompress.command("jar", "xf", jarFilePath.toAbsolutePath().toString());
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
        commandList.add("uf0");
        commandList.add(jarFilePath.toString());

        for (String classPath: classPaths) {
            commandList.add("-C");
            commandList.add(patchPath.toString());
            commandList.add(classPath);
        }

        ProcessBuilder updateJar = new ProcessBuilder(commandList);
        System.out.println(commandList);
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

        // 添加 classRootPath
        // 默认添加下面 2 条路径
        addClassRootPath("BOOT-INF/classes");
        addClassRootPath("");

        // 准备 ClassPool
        pool = ClassPool.getDefault();
        for (String classRootPath: classRootPaths) {
            pool.appendClassPath(classRootPath);
            pool.appendClassPath(destPath.toString());
        }

        // 自定义 patch 工作
        this.patch(pool);
        // 执行变更 classes 操作
        this.modity();

        if (cleanAfterPatch) {
            deleteAll(patchPath.toFile());
            deleteAll(destPath.toFile());
        }
    }

    public void packageJarFile(Path targetPath, Path sourcePath) {
        List<String> commandList = new ArrayList<>();
        commandList.add("jar");
        commandList.add("cf0M");
        commandList.add(targetPath.toAbsolutePath().toString());
        commandList.add(".");

        ProcessBuilder packageJar = new ProcessBuilder(commandList);
        packageJar.directory(sourcePath.toFile());
        System.out.println(commandList);
        try {
            Process p = packageJar.start();
            p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 自定义 patch
    public abstract void patch(ClassPool pool);

    public void modity() {
        try {
            for (PatchLibrary patchLibrary: patchLibraries) {

                String patchedPath = patchPath.resolve(patchLibrary.libName.replace(".", "_")).toString();
                List<String> classTargetPaths = new ArrayList<>();

                for (CtClass ct: patchLibrary.patchClasses) {
                    ct.writeFile(patchLibrary.extractDir.toString());
                    String patchClassPath = ct.getName().replace('.', '/');
                    String classTargetPath = patchClassPath + ".class";
                    classTargetPaths.add(classTargetPath);
                }

                packageJarFile(patchLibrary.libFile, patchLibrary.extractDir);

                deleteAll(patchLibrary.extractDir.toFile());
            }

            if (!patchPath.toFile().exists()) {
                patchPath.toFile().mkdir();
            }

            List<String> classTargetPaths = new ArrayList<>();
            classTargetPaths.add(".");

            for (PatchClass patchClass: patchClasses) {
                String patchedPath = patchPath.resolve(patchClass.patchPrefix).toString();
                patchClass.ctClass.writeFile(patchedPath);
                String patchClassPath = patchClass.ctClass.getName().replace('.', '/');
                String classTargetPath = patchClass.patchPrefix + patchClassPath + ".class";
//                classTargetPaths.add(classTargetPath);
            }

            // 更新 patch jar
            updateJarFile(jarPatchFilePath, patchPath, classTargetPaths);
        } catch (CannotCompileException | IOException e) {
            throw new RuntimeException("patch class failed: " + e);
        }


    }
}

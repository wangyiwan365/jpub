package com.javapublish;

import java.io.*;
import java.nio.charset.Charset;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Build {


    private File jarDir;
    private File jdkDir;
    private File publishDir;

    private boolean console;

    private JarFile jarFile;

    private String mainClass;

    private File mingwDir = new File(".\\MinGW-64\\bin");
    private File gccFile = new File(".\\MinGW-64\\bin\\gcc");


    public Build(File jarDir, File jdkDir, File publishDir,boolean console) {
        this.jarDir = jarDir;
        this.jdkDir = jdkDir;
        this.publishDir = publishDir;
        this.console = console;


        try {
            jarFile = new JarFile(jarDir);
            Manifest manifest = jarFile.getManifest();

            if (manifest != null) {
                // 获取主属性
                Attributes attributes = manifest.getMainAttributes();

                // 获取Main-Class属性
                mainClass = attributes.getValue("Main-Class").replace('.', '/');

                System.out.println(mainClass);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        String jreName = "runtime";


        String s1 =
                "#include <windows.h>\n" +
                        "#include <jni.h>\n" +
                        "#include <stdio.h>\n" +
                        "typedef jint(JNICALL* CreateJavaVMFunc)(JavaVM**, JNIEnv**, void*);\n" +
                        "int main(int argc, char *argv[]){\n";
        String s2 =
                "wchar_t *str1 = L\"%s\\\\" + jreName + "\\\\bin\";\n" +
                        "wchar_t *msgJVM = L\"找不到jvm.dll，错误码：%d\";\n" +
                        "wchar_t *msgFJVM = L\"jvm.dll损坏，错误码：%d\";\n" +
                        "wchar_t *msgCJVM = L\"创建JVM失败\";\n" +
                        "const char *msgMainClass = \"" + mainClass + "\";\n" +
                        "wchar_t *msgFClass = L\"找不到主类\";\n" +
                        "wchar_t *msgFMethod = L\"找不到主方法\";\n";
        String s3 = "JavaVMInitArgs vmArgs = { 0 };\n" +
                "JavaVMOption options[2];\n" +
                "vmArgs.nOptions = 2;\n" +
                "options[0].optionString = \"-Djava.class.path=" + jarDir.getName() + "\";\n";

        String s4 =
                "WCHAR szExePath[MAX_PATH] = { 0 };\n" +
                        "GetModuleFileNameW(NULL, szExePath, MAX_PATH);\n" +
                        "WCHAR* pLastBackslash = wcsrchr(szExePath, L'\\\\');\n" +
                        "*pLastBackslash = L'\\0';\n" +
                        "WCHAR szWorkDir[MAX_PATH] = { 0 };\n" +
                        "swprintf_s(szWorkDir, MAX_PATH,str1, szExePath);\n" +
                        "SetCurrentDirectoryW(szWorkDir);\n" +
                        "WCHAR szJvmPath[MAX_PATH] = { 0 };\n" +
                        "swprintf_s(szJvmPath, MAX_PATH, L\"%s\\\\server\\\\jvm.dll\", szWorkDir);\n" +
                        "HMODULE hJvm = LoadLibraryW(szJvmPath);\n" +
                        "if (!hJvm) {\n" +
                        "WCHAR errMsg[128];\n" +
                        "wsprintfW(errMsg,msgJVM,GetLastError());\n" +
                        "MessageBoxW(NULL, errMsg, L\"错误\", MB_ICONERROR | MB_OK);\n" +
                        "return 1;\n" +
                        "}\n" +
                        "CreateJavaVMFunc createJvm = (CreateJavaVMFunc)GetProcAddress(hJvm, \"JNI_CreateJavaVM\");\n" +
                        "if (!createJvm) {\n" +
                        "WCHAR errMsg[128];\n" +
                        "wsprintfW(errMsg,msgFJVM, GetLastError());\n" +
                        "MessageBoxW(NULL, errMsg, L\"错误\", MB_ICONERROR | MB_OK);\n" +
                        "FreeLibrary(hJvm);\n" +
                        "return 1;\n" +
                        "}\n" +
                        "char userDirOption[1024] = { 0 };\n" +
                        "char exePathUtf8[MAX_PATH * 4] = { 0 };\n" +
                        "WideCharToMultiByte(CP_UTF8, 0, szExePath, -1, exePathUtf8, sizeof(exePathUtf8), NULL, NULL);\n" +
                        "snprintf(userDirOption, sizeof(userDirOption), \"-Duser.dir=%s\", exePathUtf8);"+
                        "options[1].optionString = userDirOption;"+
                        "vmArgs.options = options;\n" +
                        "vmArgs.version = JNI_VERSION_1_8;\n" +
                        "vmArgs.ignoreUnrecognized = JNI_TRUE;\n" +
                        "JavaVM* jvm = NULL;\n" +
                        "JNIEnv* env = NULL;\n" +
                        "if (createJvm(&jvm, &env, &vmArgs) != JNI_OK) {\n" +
                        "MessageBoxW(NULL,msgCJVM, L\"错误\", MB_ICONERROR | MB_OK);\n" +
                        "FreeLibrary(hJvm);\n" +
                        "return 1;\n" +
                        "}\n" +
                        "SetCurrentDirectoryW(szExePath);\n" +
                        "jclass mainClass = (*env)->FindClass(env, msgMainClass);\n" +
                        "if (!mainClass) {\t\n" +
                        "MessageBoxW(NULL,msgFClass,L\"\",MB_ICONERROR|MB_OK);\n" +
                        "(*jvm)->DestroyJavaVM(jvm);\n" +
                        "FreeLibrary(hJvm);\n" +
                        "return 1;\n" +
                        "}\n" +
                        "jmethodID mainMethod = (*env)->GetStaticMethodID(env, mainClass, \"main\", \"([Ljava/lang/String;)V\");\n" +
                        "if (!mainMethod) {\n" +
                        "MessageBoxW(NULL,msgFMethod, L\"错误\", MB_ICONERROR | MB_OK);\n" +
                        "(*env)->DeleteLocalRef(env, mainClass);\n" +
                        "(*jvm)->DestroyJavaVM(jvm);\n" +
                        "FreeLibrary(hJvm);\n" +
                        "return 1;\n" +
                        "}\n" +
                        "jclass stringClass = (*env)->FindClass(env, \"java/lang/String\");\n" +
                        "int javaArgc = argc - 1;\n" +
                        "jobjectArray args = (*env)->NewObjectArray(env, javaArgc, stringClass, NULL);\n" +
                        "for (int i = 0; i < javaArgc; i++) {\n" +
                        "jstring arg = (*env)->NewStringUTF(env, argv[i + 1]);\n" +
                        "(*env)->SetObjectArrayElement(env, args, i, arg);\n" +
                        "(*env)->DeleteLocalRef(env, arg);\n" +
                        "}\n" +
                        "(*env)->CallStaticVoidMethod(env, mainClass, mainMethod,args);\n" +
                        "(*jvm)->DetachCurrentThread(jvm);\n" +
                        "(*jvm)->DestroyJavaVM(jvm);\n" +
                        "return 0;\n" +
                        "}";
        new File(".\\cache").mkdir();
        File srcFile = new File(".\\cache\\run.c");
        srcFile.delete();

        try {
            srcFile.createNewFile();
            System.out.println(srcFile.getAbsolutePath());
            OutputStreamWriter srcWriter = new OutputStreamWriter(new FileOutputStream(srcFile, true), Charset.forName("UTF-8"));
            srcWriter.write(s1);
            srcWriter.write(s2);
            srcWriter.write(s3);
            srcWriter.write(s4);
            srcWriter.close();



            String gcc = gccFile.getAbsolutePath();
            File bin = mingwDir.getAbsoluteFile();

            System.out.println(gcc);
            System.out.println(bin);


            if (!console) {



                ProcessBuilder mingw = new ProcessBuilder(gcc
                        ,"..\\..\\cache\\run.c"
                        ,"-s"
                        ,"-mwindows"
                        ,"-I\"" + jdkDir.getAbsolutePath() + "\\include\""
                        ,"-I\"" + jdkDir.getAbsolutePath() + "\\include\\win32\""
                        ,"-o"
                        ,"\"" + publishDir.getAbsolutePath() + "\\run.exe\"");
                mingw.directory(bin);
                Process process = mingw.start();



/**
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("OUT: " + line);
                    }
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("ERR: " + line);
                    }
                }

 */

                process.waitFor();

            }else{
                ProcessBuilder mingw = new ProcessBuilder(gcc
                        ,"..\\..\\cache\\run.c"
                        ,"-s"
                        ,"-I\"" + jdkDir.getAbsolutePath() + "\\include\""
                        ,"-I\"" + jdkDir.getAbsolutePath() + "\\include\\win32\""
                        ,"-o"
                        ,"\"" + publishDir.getAbsolutePath() + "\\run.exe\"");
                mingw.directory(bin);
                Process process = mingw.start();
                process.waitFor();
            }



            if (!new File(publishDir.getAbsolutePath() + "\\run.exe").exists()) {
                System.out.println("发布失败");
                //srcFile.delete();
                return;
            }


            Runtime.getRuntime().exec("xcopy \"" + jdkDir.getAbsolutePath() + "\\bin\"  \"" + publishDir.getAbsolutePath() + "\\runtime\\bin\" /E /I /Y /Q").waitFor();
            Runtime.getRuntime().exec("xcopy \"" + jdkDir.getAbsolutePath() + "\\conf\"  \"" + publishDir.getAbsolutePath() + "\\runtime\\conf\" /E /I /Y /Q").waitFor();
            Runtime.getRuntime().exec("xcopy \"" + jdkDir.getAbsolutePath() + "\\lib\"  \"" + publishDir.getAbsolutePath() + "\\runtime\\lib\" /E /I /Y /Q").waitFor();


            Runtime.getRuntime().exec("xcopy /q \"" + jarDir.getAbsolutePath() + "\" \"" + publishDir.getAbsolutePath() + "\"").waitFor();


            Runtime.getRuntime().exec("cmd /c del /q \"" + publishDir.getAbsolutePath() + "\\runtime\\bin\\*.exe\"").waitFor();
            Runtime.getRuntime().exec("cmd /c del /q \"" + publishDir.getAbsolutePath() + "\\runtime\\bin\\api-ms-win*.dll\"").waitFor();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        srcFile.delete();


    }
}


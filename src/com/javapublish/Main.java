package com.javapublish;

import java.io.File;

public class Main {



    public static void main(String[] args) {


        String jar = null;
        String jdk = null;
        String publish = null;

        boolean console = false;


        File jarDir;
        File jdkDir;
        File publishDir;

        for (int i = 0; i < args.length; i++) {
            try {
                switch (args[i]) {
                    case "-jar":
                        jar = args[i + 1];
                        break;
                    case "-jdk":
                        jdk = args[i + 1];
                        break;
                    case "-o":
                        publish = args[i + 1];
                        break;
                    case "-console":
                        console=true;
                        break;

                }
            }catch (ArrayIndexOutOfBoundsException e){
                return;
            }
        }


        if (jar==null||jdk==null||publish==null){
            System.out.println("用法：\n" +
                    "    jpub -jar <jar文件路径> -jdk <jdk路径> -o <目标目录> [-console]\n" +
                    "参数说明：\n" +
                    "    -jar     必填，指定要处理的 JAR 文件路径\n" +
                    "    -jdk     必填，指定 JDK 安装路径\n" +
                    "    -o       必填，指定发布位置（绝对路径）\n" +
                    "    -console 可选，启用控制台模式");
            return;
        }

        jarDir = new File(jar);
        jdkDir = new File(jdk);
        publishDir = new File(publish);

        if (!jarDir.exists() || !jdkDir.exists()||!publishDir.exists()){
            System.out.println("提供正确且存在的文件路径");
            return;
        }

        File publishDir2 = new File(publish+"\\publish");
        if (!publishDir2.mkdir()){
            System.out.println("目录"+publishDir2.getAbsolutePath()+"已经存在,请移除");
            return;
        }

        new Build(jarDir,jdkDir,publishDir2,console);



    }
}

package com.xlh.study.hotfixsample.tinker;

import android.content.Context;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;

/**
 * @author: Watler Xu
 * time:2020/1/17
 * description:修复BUG的管理类
 * version:0.0.1
 */
public class FixManager {

    // 创建一个装载到加载到的dex文件的File对象集合
    private static HashSet<File> loadedDex = new HashSet<>();

    static {
        // 修复之前，先清空这个集合
        loadedDex.clear();
    }

    /**
     * 根据上下文去加载替换dex文件，并存储到集合中
     *
     * @param context
     */
    public static void loadDex(Context context) {
        if (context == null) {
            return;
        }
        // 获取当前上下文的私有路径，也就是dex的文件目录
        File filesDir = context.getDir(Constants.DEX_DIR, Context.MODE_PRIVATE);
        // 通过私有路径获取路径目录中所有的文件的数据
        File[] files = filesDir.listFiles();
        // 进行遍历
        for (File file : files) {
            // 判断这个文件对象的集合是不是符合要求
            if (file.getName().startsWith("classes") || file.getName().endsWith(".dex")) {
                // 先将补丁文件放到一个集合里，然后再进行合并
                loadedDex.add(file);
            }
        }

        // 创建一个目录，用来装载解压的文件
        String optimizeDir = filesDir.getAbsolutePath() + File.separator + "opt_dex";
        File fopt = new File(optimizeDir);
        // 判断目录是否存在
        if (!fopt.exists()) {
            // 没有就创建
            fopt.mkdirs();
        }
        // 合并dex
        combineDex(context, fopt);

    }

    /**
     * 1.通过反射获取系统和自己创建的ClassLoader、pathList、dexElements[]
     * 2.创建一个新的dexElements数组，修复的dex添加在前，未修复的dex添加在后。这样无BUG的类就会先被加载，有BUG的类不会被加载，达到热修复的目的
     * 3.通过反射将新的dexElements数组放入到系统的dexElements数组中
     * @param context
     * @param fopt
     */
    private static void combineDex(Context context, File fopt) {
        // 加载应用程序的dex的DexClassLoader
        BaseDexClassLoader pathClassLoader = (BaseDexClassLoader) context.getClassLoader();
        // 循环所有找到的dex文件的集合
        for (File dex : loadedDex) {
            // 加载指定的修复的dex的DexClassLoader
            DexClassLoader dexClassLoader = new DexClassLoader(dex.getAbsolutePath(), fopt.getAbsolutePath(), null, context.getClassLoader());

            try {
                // 1.通过反射去获取系统的类加载器(此时，是未修复的dex文件的集合)
                // 获取DexClassLoader类加载的父类
                Class<?> baseDexClazzLoader = Class.forName("dalvik.system.BaseDexClassLoader");
                // 从父类的对象中的去获取pathList的成员变量
                // 获取系统的类加载器中的pathList的成员变量
                Field pathList = baseDexClazzLoader.getDeclaredField("pathList");
                // 设置这个成员变量的对象可以被访问到
                pathList.setAccessible(true);
                // 执行pathList的get方法
                Object pathListObject = pathList.get(pathClassLoader);
                // 获取pathList的类对象
                Class<?> systemPathListClass = pathListObject.getClass();
                // 通过这个类对象获取它里面的一个叫做dexElements的成员变量
                // 获取系统的类加载器中的pathList的成员变量中的dexElements的成员变量
                Field dexElementsField = systemPathListClass.getDeclaredField("dexElements");
                // 设置这个对象可以被访问
                dexElementsField.setAccessible(true);
                // 获取dexElements成员变量的值
                Object systemElements = dexElementsField.get(pathListObject);

                // 2.开始创建自己的类加载器(已经修复的dex文件的集合)
                // 通过反射获取类加载的父类
                Class<?> myDexClazzLoader = Class.forName("dalvik.system.BaseDexClassLoader");
                // 从父类的对象中去获取pathList的成员变量
                Field myPathListField = myDexClazzLoader.getDeclaredField("pathList");
                // 设置这个成员变量的对象可以被访问到
                myPathListField.setAccessible(true);
                Object myPathListObject = myPathListField.get(dexClassLoader);
                Field myDexElementsField = myPathListObject.getClass().getDeclaredField("dexElements");
                myDexElementsField.setAccessible(true);
                Object myElements = myDexElementsField.get(myPathListObject);

                // 3.把上面两个dexElements合并成新的dexElements，然后通过反射，注入到宿主的dexElements
                // 获取systemElements的类对象
                Class<?> componentType = systemElements.getClass().getComponentType();
                // 得到系统的dexElements的长度
                int systemLength = Array.getLength(systemElements);
                int myLength = Array.getLength(myElements);
                // 创建一个新的长度
                int newSystemLength = systemLength + myLength;
                // 生成一个新的数组
                Object newElementArray = Array.newInstance(componentType, newSystemLength);

                // for循环，优先添加修复后的dex，然后添加未修复的dex
                // 这样无BUG的类就会先被加载，有BUG的类不会被加载，达到热修复的目的
                for (int i = 0; i < newSystemLength; i++) {
                    if (i < myLength) {
                        Array.set(newElementArray, i, Array.get(myElements, i));
                    } else {
                        Array.set(newElementArray, i, Array.get(systemElements, i - myLength));
                    }
                }

                // 将系统的数组和我们定义的数组融合之后，再放入到系统的数组中
                Field elementField = pathListObject.getClass().getDeclaredField("dexElements");
                elementField.setAccessible(true);
                elementField.set(pathListObject,newElementArray);


            } catch (Exception e) {
                e.printStackTrace();
            }




        }
    }


}

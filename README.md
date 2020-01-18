# HotfixSample-Tinker
热修复，仿tinker原理修复BUG

### 原理
系统查找类是通过ClassLoader的FindClass遍历DexElements[]中的dex来查找的     
根据该原来，进行hook        
1.通过反射获取系统和自己创建的ClassLoader、pathList、dexElements[]
2.创建一个新的dexElements数组，修复的dex添加在前，未修复的dex添加在后。这样无BUG的类就会先被加载，有BUG的类不会被加载，达到热修复的目的
3.通过反射将新的dexElements数组放入到系统的dexElements数组中

!(演示gif)[https://github.com/CoderWalterXu/HotfixSample-Tinker/blob/master/picture/hotfix-tinker.gif]

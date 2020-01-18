package com.xlh.study.hotfixsample.tinker;

import android.app.Application;
import android.content.Context;


import androidx.multidex.MultiDexApplication;

/**
 * @author: Watler Xu
 * time:2020/1/17
 * description:
 * version:0.0.1
 */
public class MyApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 缺点是，需要重启应用，才能重新加载修复后的dex
        FixManager.loadDex(base);
    }

}

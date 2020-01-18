package com.xlh.study.hotfixsample.tinker;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnTest, btnFix;

    int i = 10;
    // 有bug时，是0,
    // 无bug时，是1。生成dex
    int a = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnTest = findViewById(R.id.btn_test);
        btnFix = findViewById(R.id.btn_fix);
        btnTest.setOnClickListener(this);
        btnFix.setOnClickListener(this);

        btnTest.setText("Bug： " + i + " / " + a);

        // 运行时权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 6.0+
            String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
            if (checkSelfPermission(perms[0]) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(perms[1]) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(perms, 100);
            }
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_test:
                test();
                break;
            case R.id.btn_fix:
                fix();
                break;
            default:
                break;
        }
    }

    private void test() {
        // 有bug时，是有BUG，
        // 无bug时，是修复成功，
        // 配合上面a的值，来验证是否替换了该类
        Toast.makeText(this, "有BUG：" + i / a, Toast.LENGTH_SHORT).show();
    }

    private void fix() {
        // assets中的文件，该文件是Build-->Make moudule app生成apk-->解压后的classes2.dex
        String name = "out.dex";

        File fileDir = getDir(Constants.DEX_DIR, Context.MODE_PRIVATE);
        AssetManager am = getResources().getAssets();
        try {
            InputStream is = am.open(name);
            String filePath = fileDir.getAbsolutePath() + File.separator + name;
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream os = new FileOutputStream(filePath);
            int len;
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.close();
            is.close();

            //粘贴完文件
            File f = new File(filePath);
            if (f.exists()) {
                //文件从sk卡赋值到应用运行目录下，成功则toast提示
                Toast.makeText(this, "dex下载成功,请重启应用", Toast.LENGTH_SHORT).show();
                FixManager.loadDex(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

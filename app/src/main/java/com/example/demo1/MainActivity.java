package com.example.demo1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.demo1.utils.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();


    }

    @Override
    protected void onResume() {
        super.onResume();

        /* 为什么需要将assets文件夹下的数据复制到新路径下？*/
        Utils.deepFile(this,"tessdata");

    }

    private void requestPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 1);

        } else {
            startActivity(new Intent(this,TakePhotoActivity.class));
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // 用户取消了权限弹窗
                if (grantResults.length == 0) {
                    Toast.makeText(this,"你取消了权限弹窗",Toast.LENGTH_SHORT).show();
                    return;
                }

                //存储所有未获取的权限 名
                StringBuilder permissonString = new StringBuilder();
                int index = 0;
                // 用户拒绝了某些权限
                for (int x : grantResults) {
                    if (x == PackageManager.PERMISSION_DENIED) {
//                        Toast.makeText(this,"部分权限未获取 ",Toast.LENGTH_SHORT).show();
//                        return;
                        permissonString.append(permissions[index]);
                    }
                    index++;
                }

                String s = permissonString.toString();
                if(!TextUtils.isEmpty(s)){
                    // 所需的权限均正常获取
                    Toast.makeText(this,"以下权限未获取 "+s,Toast.LENGTH_SHORT).show();
                }else{
                    // 所需的权限均正常获取
                    Toast.makeText(this,"权限已正常获取",Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(this,TakePhotoActivity.class));
                    finish();
                }
            }
        }
    }
}

package com.example.fbackup;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final int READ_SDCARD_REQ = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_SDCARD_REQ: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("orpr","onRequestPermissionsResult granted");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.i("orpr","onRequestPermissionsResult denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void requestPermission(Context context) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("requestPermission","checkSelfPermission");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                Log.i("requestPermission","shouldShowRequestPermissionRationale");
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        READ_SDCARD_REQ);
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_SDCARD_REQ);

            } else {
                Log.i("requestPermission","requestPermissions");
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        READ_SDCARD_REQ);
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_SDCARD_REQ);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    /**
     * You can test by follow steps:
     * 1) use the apk that you want to backup.
     * 2) backup it, you can change var "final String pkgName" to your apk name.
     * 3) run this app, then click "备份".
     * 4) make sure the backup file is generated in "/storage/emulated/0/fBackup/{pkgName}/backup.ab".
     * 5) clear the app data by "清除存储空间", then make sure the permission of your app is cleared too.(found in 应用信息->权限).
     * 6) restore this app by click "还原".
     * 7) make sure the permission is restored. (found it in 应用信息->权限).
     * 8) open your apk and make sure your data is back.
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission(this);
        Context mainContext = this;

        final String pkgName = "com.autonavi.minimap";

        Button back_button = (Button)findViewById(R.id.button);
        back_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                new FullBackup().backupApk(mainContext, pkgName);
            }
        });

        Button restore_button = (Button)findViewById(R.id.button2);
        restore_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                new FullBackup().restoreApk(mainContext, pkgName);
            }
        });
    }
}
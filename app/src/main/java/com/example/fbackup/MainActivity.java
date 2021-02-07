package com.example.fbackup;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
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


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void grant(PackageManager packageManager, String pkgName) {
        try {
            int uid = packageManager.getApplicationInfo("com.android.settings", 0).uid;
            UserHandle userHandle = UserHandle.getUserHandleForUid(uid);

            Class<?> refPackageManager = packageManager.getClass();
            Method refGrantRuntimePermission = refPackageManager.getDeclaredMethod("grantRuntimePermission", String.class, String.class, UserHandle.class);
            refGrantRuntimePermission.setAccessible(true);
            refGrantRuntimePermission.invoke(packageManager, pkgName, Manifest.permission.READ_EXTERNAL_STORAGE, userHandle);
        } catch (Exception e) {

        }
    }

    private void checkPermission(String pkgName) {
        StringBuffer appNameAndPermissions=new StringBuffer();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo applicationInfo : packages) {
            if(!applicationInfo.packageName.equals(pkgName))
                continue;
            Log.d("test", "App: " + applicationInfo.name + " Package: " + applicationInfo.packageName);
            try {
                PackageInfo packageInfo = pm.getPackageInfo(applicationInfo.packageName, PackageManager.GET_PERMISSIONS);
                appNameAndPermissions.append(packageInfo.packageName+"*:\n");
                //Get Permissions
                String[] requestedPermissions = packageInfo.requestedPermissions;
                if(requestedPermissions != null) {
                    for (int i = 0; i < requestedPermissions.length; i++) {
                        Log.d("test", requestedPermissions[i]);
                        appNameAndPermissions.append(requestedPermissions[i]+"\n");
                    }
                    appNameAndPermissions.append("\n");
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission(this);

        Button back_button = (Button)findViewById(R.id.button);
        back_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                //new FullBackup().backupApk("com.autonavi.minimap");

                checkPermission("com.autonavi.minimap");

                PackageManager pm = getPackageManager();
                grant(pm, "com.autonavi.minimap");
                //revokeRuntimePermission
            }
        });

        Button restore_button = (Button)findViewById(R.id.button2);
        restore_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //new FullBackup().restoreApk("com.autonavi.minimap");
            }
        });
    }
}
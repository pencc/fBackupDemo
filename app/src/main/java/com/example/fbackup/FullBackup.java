package com.example.fbackup;

import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class FullBackup {
    public void backupApk(String mPkgName) {
        try {
            // /storage/emulated/0/backup/{pkgname}/backup.ab
            String sdPath = Environment.getExternalStorageDirectory().getPath();
            String backupDirPath = sdPath + "/backup/" + mPkgName;
            File dir = new File(backupDirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String backupFilePath = sdPath + "/backup/" + mPkgName + "/backup.ab";
            File file = new File(backupFilePath);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file = new File(backupFilePath);
            ArrayList pkgList = new ArrayList();
            pkgList.add(mPkgName);
            //原理是反射调用BackupManagerService.fullbackup
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            Method getService = serviceManager.getMethod("getService", String.class);
            getService.setAccessible(true);
            IBinder iBinder = (IBinder) getService.invoke(null, "backup");
            Class<?> sub = Class.forName("android.app.backup.IBackupManager$Stub");
            Method asInterface = sub.getDeclaredMethod("asInterface", IBinder.class);
            asInterface.setAccessible(true);
            Object backupManager = asInterface.invoke(null, iBinder);
            if (backupManager == null) {
                //addLog("backManager is null");
                return;
            }
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
                String[] strArr = new String[pkgList.size()];
                Method[] methods = backupManager.getClass().getMethods();
                int length = methods.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    Method method = methods[i];
                    if ("adbBackup".equals(method.getName())) {
                        //需要android.permission.BACKUP权限，hook权限检查
//                           //fullBackup: uid 10065 does not have android.permission.BACKUP.
//                            android.Manifest.permission.BACKUP

                        //fullBackup原方法及参数
                        boolean includeApks = false;
                        //obb文件
                        boolean includeObbs = false;
                        //sdcard文件
                        boolean includeShared = false;
                        //是否所有app
                        boolean doAllApps = false;
                        //是否包括系统
                        boolean includeSystem = false;
                        method.setAccessible(true);
                        method.invoke(backupManager, new Object[]{parcelFileDescriptor,
                                Boolean.valueOf(includeApks), Boolean.valueOf(includeObbs),
                                Boolean.valueOf(includeShared), Boolean.valueOf(doAllApps),
                                Boolean.valueOf(includeSystem), (String[]) pkgList.toArray(strArr)});
                        break;
                    }
                    i++;                }
            } catch (Exception e2) {
                //addLog("Unable to invoke backup manager for backup" + e2.getMessage());
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e3) {
                    }
                }
            } finally {
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e4) {
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            //addLog("ClassNotFoundException : " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            //addLog("NoSuchMethodException : " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            //addLog("IllegalAccessException : " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            //addLog("InvocationTargetException : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

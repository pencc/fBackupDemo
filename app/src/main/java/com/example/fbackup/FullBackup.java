package com.example.fbackup;

import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class FullBackup {
    public void backupApk(String mPkgName) {
        try {
            // /storage/emulated/0/backup/{pkgname}/backup.ab
            String sdPath = Environment.getExternalStorageDirectory().getPath();
            String backupDirPath = sdPath + "/fBackup/" + mPkgName;
            File dir = new File(backupDirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String[] pkgList = {mPkgName};

            String backupFilePath = sdPath + "/fBackup/" + mPkgName + "/backup2.ab";
            File file = new File(backupFilePath);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("backup", e.getMessage());
                }
            }
            file = new File(backupFilePath);
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
                        boolean includeApks = true;
                        //obb文件
                        boolean includeObbs = false;
                        //sdcard文件
                        boolean includeShared = true;
                        boolean doWidgets = false;
                        //是否所有app
                        boolean doAllApps = false;
                        //是否包括系统
                        boolean includeSystem = false;
                        boolean compress = true;
                        boolean doKeyValue = false;
                        method.setAccessible(true);
                        method.invoke(backupManager, new Object[]{parcelFileDescriptor,
                                Boolean.valueOf(includeApks), Boolean.valueOf(includeObbs),
                                Boolean.valueOf(includeShared), Boolean.valueOf(doWidgets),
                                Boolean.valueOf(doAllApps), Boolean.valueOf(includeSystem),
                                Boolean.valueOf(compress), Boolean.valueOf(doKeyValue),
                                pkgList});
                        break;
                    }
                    i++;                }
            } catch (Exception e2) {
                //addLog("Unable to invoke backup manager for backup" + e2.getMessage());
                Log.e("backup", "fail to backup:" + e2.getMessage());
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
            Log.e("backup", "fail to backup:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            //addLog("NoSuchMethodException : " + e.getMessage());
            e.printStackTrace();
            Log.e("backup", "fail to backup:" + e.getMessage());
        } catch (IllegalAccessException e) {
            //addLog("IllegalAccessException : " + e.getMessage());
            e.printStackTrace();
            Log.e("backup", "fail to backup:" + e.getMessage());
        } catch (InvocationTargetException e) {
            //addLog("InvocationTargetException : " + e.getMessage());
            e.printStackTrace();
            Log.e("backup", "fail to backup:" + e.getMessage());
        }
    }

    private void nioTransferCopy(File source, File target) {
        FileChannel in = null;
        FileChannel out = null;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = new FileInputStream(source);
            outStream = new FileOutputStream(target);
            in = inStream.getChannel();
            out = outStream.getChannel();
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inStream.close();
                in.close();
                outStream.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void restoreApk(String mPkgName) {
        try {
            String sdPath = Environment.getExternalStorageDirectory().getPath();
            String SourceFilePath = sdPath + "/fBackup/" + mPkgName + "/backup2.ab";
            String backupFilePath = sdPath + "/fBackup/" + mPkgName + "/backup.ab";
            File sfile = new File(SourceFilePath);
            File file = new File(backupFilePath);
            nioTransferCopy(sfile, file);

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
                parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                Method[] methods = backupManager.getClass().getMethods();
                int length = methods.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    Method method = methods[i];
                    if ("adbRestore".equals(method.getName())) {
                        //需要android.permission.BACKUP权限，hook权限检查
//                           //fullBackup: uid 10065 does not have android.permission.BACKUP.
//                            android.Manifest.permission.BACKUP

                        method.setAccessible(true);
                        method.invoke(backupManager, parcelFileDescriptor);
                        break;
                    }
                    i++;                }
            } catch (Exception e2) {
                Log.e("restore", "fail to restore:" + e2.getMessage());
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
                file.delete();
            }
        } catch (ClassNotFoundException e) {
            Log.e("restore", "fail to restore:" + e.getMessage());
            //addLog("ClassNotFoundException : " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e("restore", "fail to restore:" + e.getMessage());
            //addLog("NoSuchMethodException : " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e("restore", "fail to restore:" + e.getMessage());
            //addLog("IllegalAccessException : " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e("restore", "fail to restore:" + e.getMessage());
            //addLog("InvocationTargetException : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

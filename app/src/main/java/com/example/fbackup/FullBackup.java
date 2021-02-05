package com.example.fbackup;

import android.os.Environment;
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

public class FullBackup {

    private void copyFile_channelCopy(String src_file, String dst_file) {
        FileInputStream fin = null;
        FileOutputStream fout = null;
        FileChannel finChannel = null;
        FileChannel foutChannel = null;
        try {
            fin = new FileInputStream(src_file);
            fout = new FileOutputStream(dst_file);

            finChannel = fin.getChannel();
            foutChannel = fout.getChannel();

            //直接把通道进行数据传输
            // transferTo 和 transferFrom两者选1即可
//        finChannel.transferTo(0,finChannel.size(),foutChannel);
            foutChannel.transferFrom(finChannel, 0, finChannel.size());
            //正式写法是需要写在 try-catch-finally 中的，处理仅作为学习
            finChannel.close();
            foutChannel.close();
            fin.close();
            fout.close();
        } catch (Exception e) {
            // todo
            Log.e("FullBackup", "fail to copy back");
        }
    }

    // the apk{mPkgName} will backup to /storage/emulated/0/fBackup/{mPkgName}/backup.ab
    // "/storage/emulated/0/fTmp/{mPkgName}/" is the tmp dir in backuping and restoring
    public void backupApk(String mPkgName) {
        // /storage/emulated/0/backup/{pkgname}/backup.ab
        String sdPath = Environment.getExternalStorageDirectory().getPath();
        try {
            String backupDirPath = sdPath + "/fTmp/" + mPkgName;
            File dir = new File(backupDirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String[] pkgList = {mPkgName};

            String backupFilePath = sdPath + "/fTmp/" + mPkgName + "/backup.ab";
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
                        boolean includeApks = false;
                        boolean includeObbs = false;
                        boolean includeShared = false;
                        boolean doWidgets = false;
                        boolean doAllApps = false;
                        boolean includeSystem = true;
                        boolean compress = true;
                        boolean doKeyValue = false;
                        method.setAccessible(true);
                        method.invoke(backupManager, new Object[]{parcelFileDescriptor,
                                Boolean.valueOf(includeApks), Boolean.valueOf(includeObbs),
                                Boolean.valueOf(includeShared), Boolean.valueOf(doWidgets),
                                Boolean.valueOf(doAllApps), Boolean.valueOf(includeSystem),
                                Boolean.valueOf(compress), Boolean.valueOf(doKeyValue),
                                pkgList});

                        // copy backup file to our backupDir
                        String backupDir = sdPath + "/fBackup/" + mPkgName;
                        File bdir = new File(backupDir);
                        if (!bdir.exists()) {
                            bdir.mkdirs();
                        }
                        copyFile_channelCopy(sdPath + "/fTmp/" + mPkgName + "/backup.ab",
                                sdPath + "/fBackup/" + mPkgName + "/backup.ab");
                        // delete orig file to free space
                        file.delete();
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

    public void restoreApk(String mPkgName) {
        try {
            String sdPath = Environment.getExternalStorageDirectory().getPath();

            copyFile_channelCopy(sdPath + "/fBackup/" + mPkgName + "/backup.ab",
                    sdPath + "/fTmp/" + mPkgName + "/backup.ab");

            String backupFilePath = sdPath + "/fTmp/" + mPkgName + "/backup.ab";
            File file = new File(backupFilePath);

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

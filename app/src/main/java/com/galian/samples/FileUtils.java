package com.galian.samples;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static String currentTimeStrCompact() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return dateFormat.format(new Date());
    }

    public static String currentTimeStr(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        return dateFormat.format(new Date());
    }

    public static boolean saveFile(Context context, String fileName, String content) {
        File baseDir = context.getExternalFilesDir(null);
        if (fileName == null || fileName.isEmpty()) {
            fileName = fileName.trim();
            fileName = fileName + "_" + currentTimeStrCompact() + ".txt";
        }
        File destFile = new File(baseDir, fileName);
        if (destFile.exists()) {
            fileName = currentTimeStrCompact() + "_" + fileName;
            destFile = new File(baseDir, fileName);
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(destFile);
            outputStream.write(content.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return copyAndroidDataToSdcardVisibleDir(context, destFile.getPath(),
                "/storage/emulated/0/Download/", fileName);
    }

    public static boolean copyAndroidDataToSdcardVisibleDir(Context context, String filePath, String destPath, String newName) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(filePath);
            if (TextUtils.isEmpty(newName)) {
                int pos = filePath.lastIndexOf("/");
                if (pos == -1) {
                    newName = filePath;
                } else {
                    newName = filePath.substring(pos + 1);
                }
            }
            String destfilePath;
            File destDir = new File(destPath);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            if (destPath.endsWith("/")) {
                destfilePath = destPath + newName;
            } else {
                destfilePath = destPath + "/" + newName;
            }
            Log.d(TAG, "destfilePath: " + destfilePath);
            Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                    null, "_data = ?", new String[]{destfilePath}, null);
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, destfilePath + " already exists.");
                int lastSlashPos = destfilePath.lastIndexOf("/");
                destfilePath = destfilePath.substring(0, lastSlashPos)
                        + "/" + currentTimeStrCompact() + "_"
                        + destfilePath.substring(lastSlashPos + 1);
                cursor.close();
            } else if (cursor != null) {
                Log.d(TAG, "No duplicate file. close cursor.");
                cursor.close();
            }
            ContentValues values = new ContentValues();
            values.put("_data", destfilePath);
            values.put("_display_name", newName);
            Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri != null) {
                Log.d(TAG, "uri: " + uri);
                out = context.getContentResolver().openOutputStream(uri);
            } else {
                Log.d(TAG, "uri is null");
            }
            int count = 0;
            int bufSize = 8192;
            byte[] tempBuf = new byte[bufSize];
            while ((count = in.read(tempBuf, 0, bufSize)) > 0) {
                out.write(tempBuf, 0, count);
            }
            out.flush();
            Log.d(TAG, "file " + filePath + " is copied to " + destfilePath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}

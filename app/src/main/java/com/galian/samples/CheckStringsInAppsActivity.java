package com.galian.samples;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class CheckStringsInAppsActivity extends Activity {

    private static final String TAG = "CheckStrings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_strings_in_apps);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.check_strings)
    void checkStrings() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                checkStringsAsync();
            }
        }, "checkStringsThread").start();
    }

    void checkStringsAsync() {
        PackageManager pm = getPackageManager();

        ArrayList<PackageInfo> thirdPartyAppList = new ArrayList<>();

        List<PackageInfo> pkgList = pm.getInstalledPackages(0);
        if (pkgList != null && pkgList.size() > 0) {
            for (PackageInfo pi : pkgList) {
                Log.e(TAG, "pkg name: " + pi.packageName);
                Log.e(TAG, "dir: " + pi.applicationInfo.publicSourceDir);
                if (pi.applicationInfo.publicSourceDir.startsWith("/data/app/")
                        && !pi.packageName.startsWith("com.smartisanos.")) {
                    thirdPartyAppList.add(pi);
                }
            }
        } else {
            Log.e(TAG, "No packages");
        }
/*
        if (thirdPartyAppList.size() > 0) {
            Log.e(TAG, "====================================================");
            for (PackageInfo pi : thirdPartyAppList) {
                //Log.e(TAG, "pkg name: " + pi.packageName);
                Log.e(TAG, "dir: " + pi.applicationInfo.publicSourceDir);
            }
        }
*/
        ArrayList<String> RClassNameList = new ArrayList<>();

        if (thirdPartyAppList.size() > 0) {

            for (PackageInfo pi : thirdPartyAppList) {
                String pkgName = pi.packageName;
                Log.e(TAG, "======================== " + pkgName + " ========================");

                try {
                    Context context = createPackageContext(pkgName, CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
                    ClassLoader pathClassLoader = context.getClassLoader();

                    Log.e(TAG, "-----------------------------------------------------");
                    Class baseDexClassLoaderCls = Class.forName("dalvik.system.BaseDexClassLoader");
                    Field pathListField = baseDexClassLoaderCls.getDeclaredField("pathList");
                    pathListField.setAccessible(true);
                    Object dexPathListObj = pathListField.get(pathClassLoader);

                    Class dexPahtListCls = dexPathListObj.getClass();
                    Field dexElementsField = dexPahtListCls.getDeclaredField("dexElements");
                    dexElementsField.setAccessible(true);
                    Object elementArrayObj = dexElementsField.get(dexPathListObj);
                    //Class type = elementArrayObj.getClass().getComponentType();
                    int len = Array.getLength(elementArrayObj);

                    RClassNameList.clear();

                    for (int i = 0; i < len; i++) {
                        Object elementObj = Array.get(elementArrayObj, i);

                        Class ElementCls = elementObj.getClass();
                        Field dexFileField = ElementCls.getDeclaredField("dexFile");
                        dexFileField.setAccessible(true);
                        Object dexFileObj = dexFileField.get(elementObj);
                        if (dexFileObj != null) {
                            Class dexFileCls = dexFileObj.getClass();
                            Method DexFile_entries = dexFileCls.getMethod("entries");
                            Enumeration<String> e = (Enumeration<String>) DexFile_entries.invoke(dexFileObj);
                            while (e.hasMoreElements()) {
                                String className = e.nextElement();
                                //Log.e(TAG, className);
                                if (className.contains(".R$string")) {
                                    Log.e(TAG, className);
                                    RClassNameList.add(className);
                                }
                            }
                        }
                    }
                    Log.e(TAG, "-----------------------------------------------------");

                    Resources resources = context.getResources();
                    if (RClassNameList.size() > 0) {
                        for (String RClassName : RClassNameList) {
                            String Rclass = RClassName; //pkgName + ".R$string";
                            Log.e(TAG, "R class name: " + Rclass);
                            Class RClassObj = context.getClassLoader().loadClass(Rclass);
                            ReflectUtils reflectUtils = new ReflectUtils(RClassObj);

                            List<Integer> ids = reflectUtils.getAllIntValues();
                            if (ids != null && ids.size() > 0) {
                                for (Integer id : ids) {
                                    Log.e(TAG, "id (hex): " + Integer.toHexString(id) + ", id: " + id);
                                    try {
                                        String str = resources.getString(id);
                                        Log.e(TAG, "str: " + str);
                                        if (str.contains("网络链接较慢，建议检查网络设置或稍后重试。")) {
                                            Log.e(TAG, pi.toString());
                                            Log.e(TAG, "+++++ package: " + pi.packageName);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(CheckStringsInAppsActivity.this, "Found it.", Toast.LENGTH_LONG).show();
                                                }
                                            });

                                        }
                                    } catch (Exception e) {
                                        //e.printStackTrace();
                                        Log.e(TAG, e.getMessage());
                                    }
                                }
                            } else {
                                Log.e(TAG, "No ids.");
                            }
                        }
                    }
                } catch (SecurityException e) {
                    //e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (PackageManager.NameNotFoundException e) {
                    //e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (ClassNotFoundException e) {
                    //e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CheckStringsInAppsActivity.this, "Done.", Toast.LENGTH_LONG).show();
            }
        });

        Log.e(TAG, "Done!!!!!!");
    }
}

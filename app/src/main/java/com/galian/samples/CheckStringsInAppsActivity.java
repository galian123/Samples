package com.galian.samples;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.galian.samples.databinding.ActivityCheckStringsInAppsBinding;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class CheckStringsInAppsActivity extends Activity {

    private static final String TAG = "CheckStrings";
    private ActivityCheckStringsInAppsBinding mBinding;
    private String mStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCheckStringsInAppsBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initViews();
        mBinding.systemApps.setChecked(true);

        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = getPackageManager().getApplicationInfo("com.samsung.android.forest", 0);
            Resources res3 = getPackageManager().getResourcesForApplication(applicationInfo);
            AssetManager assetManager = res3.getAssets();
            XmlResourceParser parser = assetManager.openXmlResourceParser("res/values/public.xml");
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        Log.e(TAG, "tagName: " + tagName);
                        if (tagName.equals("public")) {
                            String type = parser.getAttributeValue(null, "type");
                            if (type.equals("string")) {
                                String name = parser.getAttributeValue(null, "name");
                                String id = parser.getAttributeValue(null, "id");
                                Log.e(TAG, "type: " + type + ", name: " + name + ", id: " + id);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                event = parser.next();
            }
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

    }

    private void updateTips() {
        if (mBinding.systemApps.isChecked() && mBinding.nonSystemApps.isChecked()) {
            mBinding.tips.setText("Search all apps (sys & non-sys apps)");
        } else if (mBinding.systemApps.isChecked()) {
            mBinding.tips.setText("Search system apps only");
        } else if (mBinding.nonSystemApps.isChecked()) {
            mBinding.tips.setText("Search non-system apps only");
        } else {
            mBinding.tips.setText("At least 'system' or 'non-system' should be checked");
        }
    }

    private void initViews() {
        mBinding.checkStrings.setOnClickListener(v -> checkStrings());
        mBinding.systemApps.setOnCheckedChangeListener((v, isChecked) -> updateTips());
        mBinding.nonSystemApps.setOnCheckedChangeListener((v, isChecked) -> updateTips());
    }

    void checkStrings() {
        mStr = mBinding.searchedStr.getText().toString();
        if (TextUtils.isEmpty(mStr)) {
            Toast.makeText(this, "Please input string !!", Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBinding.systemApps.isChecked()
                && !mBinding.nonSystemApps.isChecked()) {
            Toast.makeText(this,
                    "At least one of 'System apps' and 'Non-system apps' should be checked.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(this::checkStringsAsync, "checkStringsThread").start();
    }

    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_ONLY_SYS_APP = 2;
    private static final int SCOPE_ONLY_NON_SYS_APP = 3;

    void checkStringsAsync() {
        int scope = 0;
        List<PackageInfo> filteredAppList = new ArrayList<>();

        int flags = PackageManager.MATCH_DISABLED_COMPONENTS;
        if (mBinding.systemApps.isChecked() && mBinding.nonSystemApps.isChecked()) {
            scope = SCOPE_ALL;
        } else if (mBinding.systemApps.isChecked()) {
            scope = SCOPE_ONLY_SYS_APP;
            flags |= PackageManager.MATCH_SYSTEM_ONLY;
        } else if (mBinding.nonSystemApps.isChecked()) {
            scope = SCOPE_ONLY_NON_SYS_APP;
        }
        PackageManager pm = getPackageManager();
        runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                "Finding " + mStr + " ...", Toast.LENGTH_LONG).show());

        boolean showDebugLog = mBinding.showMoreLog.isChecked();
        List<PackageInfo> pkgList = pm.getInstalledPackages(flags);
        if (scope == SCOPE_ALL || scope == SCOPE_ONLY_SYS_APP) {
            filteredAppList = pkgList;
        } else if (scope == SCOPE_ONLY_NON_SYS_APP) {
            if (pkgList != null && pkgList.size() > 0) {
                for (PackageInfo pi : pkgList) {
                    if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        filteredAppList.add(pi);
                    }
                }
            }
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
        ArrayList<String> rClassNameList = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();

        boolean isFound = false;
        if (filteredAppList.size() > 0) {
            int checkedPkgCnt = 0;
            int totalPkgCnt = filteredAppList.size();
            runOnUiThread(() -> mBinding.progress.setText("0/" + totalPkgCnt));
            stringBuilder.append("Check ").append(filteredAppList.size()).append(" apps");
            Log.e(TAG, "Check " + filteredAppList.size() + " apps");
            int foundCnt = 0;
            for (PackageInfo pi : filteredAppList) {
                String pkgName = pi.packageName;
                Log.e(TAG, "======================== " + pkgName + " ========================");

                try {
                    Context context = createPackageContext(pkgName, CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
                    ClassLoader pathClassLoader;
                    try {
                        pathClassLoader = context.getClassLoader();
                    } catch (Throwable t){
                        Log.e(TAG, t.getMessage());
                        checkedPkgCnt++;
                        int finalCheckedPkgCnt = checkedPkgCnt;
                        runOnUiThread(() -> mBinding.progress.setText(finalCheckedPkgCnt + "/" + totalPkgCnt));
                        continue;
                    }

                    if (showDebugLog)
                        Log.e(TAG, "-----------------------------------------------------");
                    Class<?> baseDexClassLoaderCls = Class.forName("dalvik.system.BaseDexClassLoader");
                    Field pathListField = baseDexClassLoaderCls.getDeclaredField("pathList");
                    pathListField.setAccessible(true);
                    Object dexPathListObj = pathListField.get(pathClassLoader);

                    Class<?> dexPahtListCls = dexPathListObj.getClass();
                    Field dexElementsField = dexPahtListCls.getDeclaredField("dexElements");
                    dexElementsField.setAccessible(true);
                    Object elementArrayObj = dexElementsField.get(dexPathListObj);
                    //Class type = elementArrayObj.getClass().getComponentType();
                    int len = Array.getLength(elementArrayObj);

                    rClassNameList.clear();

                    for (int i = 0; i < len; i++) {
                        Object elementObj = Array.get(elementArrayObj, i);
                        Class<?> ElementCls = elementObj.getClass();
                        Field dexFileField = ElementCls.getDeclaredField("dexFile");
                        dexFileField.setAccessible(true);
                        Object dexFileObj = dexFileField.get(elementObj);
                        if (dexFileObj != null) {
                            Class<?> dexFileCls = dexFileObj.getClass();
                            Method DexFile_entries = dexFileCls.getMethod("entries");
                            Enumeration<String> e = (Enumeration<String>) DexFile_entries.invoke(dexFileObj);
                            while (e.hasMoreElements()) {
                                String className = e.nextElement();
                                //Log.e(TAG, className);
                                if (className.contains(".R$string")) {
                                    if (showDebugLog) Log.e(TAG, className);
                                    rClassNameList.add(className);
                                }
                            }
                        }
                    }
                    if (showDebugLog)
                        Log.e(TAG, "-----------------------------------------------------");

                    Resources resources = context.getResources();
                    Locale locale = resources.getConfiguration().locale;

                    boolean needCheckRes2 = false;
                    Resources resource2 = null;
                    Configuration configuration = new Configuration();
                    if (locale.getLanguage().equals(Locale.CHINESE.getLanguage())) {
                        configuration.setLocale(Locale.ENGLISH);
                        needCheckRes2 = true;
                    } else if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                        configuration.setLocale(Locale.CHINESE);
                        needCheckRes2 = true;
                    }

                    if (needCheckRes2) {
                        Context context2 = context.createConfigurationContext(configuration);
                        resource2 = context2.getResources();
                    }
                    if (rClassNameList.size() > 0) {
                        for (String rClassName : rClassNameList) {
                            if (rClassName.equals("android.support.v7.appcompat.R$string")) {
                                continue;
                            }
                            if (showDebugLog) Log.e(TAG, "R class name: " + rClassName);
                            Class<?> rClassObj = context.getClassLoader().loadClass(rClassName);
                            List<Integer> ids = ReflectUtils.getAllStaticFinalIntValues(rClassObj);
                            if (ids.size() > 0) {
                                for (Integer id : ids) {
                                    if (showDebugLog)
                                        Log.e(TAG, "id (hex): " + Integer.toHexString(id) + ", id: " + id);
                                    try {
                                        String str = resources.getString(id);
                                        if (showDebugLog) Log.e(TAG, "str: " + str);

                                        String str2 = "";
                                        if (needCheckRes2) {
                                            str2 = resource2.getString(id);
                                            if (showDebugLog) Log.e(TAG, "str2: " + str2);
                                        }
                                        if (str.contains(mStr)
                                                || str.toLowerCase().contains(mStr.toLowerCase())) {
                                            Log.e(TAG, pi.toString());
                                            Log.e(TAG, "+++++ package: " + pi.packageName);
                                            foundCnt++;
                                            int finalFoundCnt = foundCnt;
                                            runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                                                    "Found it. " + finalFoundCnt, Toast.LENGTH_LONG).show());
                                            LocaleList localeList = resources.getConfiguration().getLocales();
                                            stringBuilder.append("\npackage: ").append(pi.packageName)
                                                    .append("\n id(hex): 0x").append(Integer.toHexString(id))
                                                    .append("\n id(dec): ").append(id)
                                                    .append("\n str(").append(localeList.get(0)).append("): ").append(str)
                                                    .append("\n str(").append(configuration.getLocales().get(0)).append("): ")
                                                    .append(str2).append("\n");
                                            isFound = true;
                                        }
                                        if (needCheckRes2) {
                                            if (str2.contains(mStr)
                                                    || str2.toLowerCase().contains(mStr.toLowerCase())) {
                                                Log.e(TAG, pi.toString());
                                                Log.e(TAG, "+++++ package: " + pi.packageName);
                                                foundCnt++;
                                                int finalFoundCnt = foundCnt;
                                                runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                                                        "Found it. " + finalFoundCnt, Toast.LENGTH_LONG).show());
                                                LocaleList localeList = resources.getConfiguration().getLocales();
                                                stringBuilder.append("\npackage: ").append(pi.packageName)
                                                        .append("\n id(hex): 0x").append(Integer.toHexString(id))
                                                        .append("\n id(dec): ").append(id)
                                                        .append("\n str(").append(configuration.getLocales().get(0)).append("): ").append(str2)
                                                        .append("\n str(").append(localeList.get(0)).append("): ")
                                                        .append(str).append("\n");
                                                isFound = true;
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, e.getMessage());
                                    }
                                }
                            } else {
                                Log.e(TAG, "No ids.");
                            }
                        }
                    }
                    // Check application name
                    String label = (String) context.getPackageManager().getApplicationLabel(pi.applicationInfo);
                    String label2 = "";
                    if (pi.applicationInfo.labelRes != 0 && resource2 != null) {
                        label2 = resource2.getString(pi.applicationInfo.labelRes);
                    }
                    LocaleList localeList = resources.getConfiguration().getLocales();

                    if (label.contains(mStr)
                            || label.toLowerCase().contains(mStr.toLowerCase())) {
                        Log.e(TAG, pi.toString());
                        Log.e(TAG, "+++++ package: " + pi.packageName);
                        foundCnt++;
                        int finalFoundCnt = foundCnt;
                        runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                                "Found it. " + finalFoundCnt, Toast.LENGTH_LONG).show());
                        stringBuilder.append("\npackage: ").append(pi.packageName)
                                .append("\n app name (").append(localeList.get(0)).append("): ").append(label)
                                .append("\n app name (").append(configuration.getLocales().get(0)).append("): ")
                                .append(label2).append("\n");
                        isFound = true;
                    }
                    if (label2.contains(mStr)
                            || label2.toLowerCase().contains(mStr.toLowerCase())) {
                        Log.e(TAG, pi.toString());
                        Log.e(TAG, "+++++ package: " + pi.packageName);
                        foundCnt++;
                        int finalFoundCnt = foundCnt;
                        runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                                "Found it. " + finalFoundCnt, Toast.LENGTH_LONG).show());
                        stringBuilder.append("\npackage: ").append(pi.packageName)
                                .append("\n app name (").append(configuration.getLocales().get(0)).append("): ")
                                .append(label2)
                                .append("\n app name (").append(localeList.get(0)).append("): ")
                                .append(label).append("\n");
                        isFound = true;
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, e.getMessage());
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    Log.e(TAG, e.getMessage());
                }
                checkedPkgCnt++;
                int finalCheckedPkgCnt = checkedPkgCnt;
                runOnUiThread(() -> mBinding.progress.setText(finalCheckedPkgCnt + "/" + totalPkgCnt));
            }
        }
        runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                "Done.", Toast.LENGTH_LONG).show());
        Log.e(TAG, "Done!!!!!!");
        if (!isFound) {
            stringBuilder.append("\nNot found.");
        }
        runOnUiThread(() -> mBinding.result.setText(stringBuilder.toString()));
    }
}


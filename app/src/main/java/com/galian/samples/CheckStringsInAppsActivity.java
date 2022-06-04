package com.galian.samples;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.galian.samples.databinding.ActivityCheckStringsInAppsBinding;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
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
        requestRuntimePermissions();
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
        mBinding.searchedStr.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                checkStrings();
                return true;
            }
            return false;
        });
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

    void checkStringsAsync() {
        if (mBinding.findByBruteForce.isChecked()) {
            findStringsBruteForce();
        } else {
            findStringsInRClass();
        }
    }

    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_ONLY_SYS_APP = 2;
    private static final int SCOPE_ONLY_NON_SYS_APP = 3;

    void findStringsInRClass() {
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

        ArrayList<String> rClassNameList = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();

        int foundCnt = 0;
        if (filteredAppList.size() > 0) {
            int checkedPkgCnt = 0;
            int totalPkgCnt = filteredAppList.size();
            runOnUiThread(() -> mBinding.progress.setText("0/" + totalPkgCnt));
            stringBuilder.append("Check ").append(filteredAppList.size()).append(" apps");
            Log.e(TAG, "Check " + filteredAppList.size() + " apps");
            for (PackageInfo pi : filteredAppList) {
                String pkgName = pi.packageName;
                Log.e(TAG, "======================== " + pkgName + " ========================");

                try {
                    Context context = createPackageContext(pkgName, CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
                    ClassLoader pathClassLoader;
                    try {
                        pathClassLoader = context.getClassLoader();
                    } catch (Throwable t) {
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
                    LocaleList localeList = resources.getConfiguration().getLocales();
                    Locale locale = localeList.get(0);

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

                    Locale locale2 = configuration.getLocales().get(0);
                    if (needCheckRes2) {
                        Context context2 = context.createConfigurationContext(configuration);
                        resource2 = context2.getResources();
                    }
                    if (rClassNameList.size() > 0) {
                        HashSet<Integer> idPool = new HashSet<>();
                        for (String rClassName : rClassNameList) {
                            if (rClassName.equals("android.support.v7.appcompat.R$string")) {
                                continue;
                            }
                            if (showDebugLog) Log.e(TAG, "R class name: " + rClassName);
                            Class<?> rClassObj = context.getClassLoader().loadClass(rClassName);
                            List<Integer> ids = ReflectUtils.getAllStaticFinalIntValues(rClassObj);
                            if (ids.size() > 0) {
                                for (Integer id : ids) {
                                    if (idPool.contains(id)) {
                                        continue;
                                    } else {
                                        idPool.add(id);
                                    }
                                    if (showDebugLog)
                                        Log.e(TAG, "id (hex): " + Integer.toHexString(id) + ", id: " + id);
                                    try {
                                        boolean matched = false;
                                        String str = resources.getString(id);
                                        if (showDebugLog) Log.e(TAG, "str: " + str);
                                        if (str.contains(mStr)
                                                || str.toLowerCase().contains(mStr.toLowerCase())) {
                                            matched = true;
                                        }

                                        String str2 = "";
                                        if (needCheckRes2) {
                                            str2 = resource2.getString(id);
                                            if (showDebugLog) Log.e(TAG, "str2: " + str2);
                                            if (str2.contains(mStr)
                                                    || str2.toLowerCase().contains(mStr.toLowerCase())) {
                                                matched = true;
                                            }
                                        }
                                        if (matched) {
                                            Log.e(TAG, "+++++ package: " + pi.packageName);
                                            foundCnt++;
                                            stringBuilder.append("\n--------------------\n ")
                                                    .append(addBlueColor(foundCnt + ". package: ")).append(pi.packageName)
                                                    .append("\n ").append(addBlueColor("id(hex): "))
                                                    .append("0x").append(Integer.toHexString(id))
                                                    .append("\n ").append(addBlueColor("id(dec): ")).append(id)
                                                    .append("\n ").append(addBlueColor("str(" + locale + "): "))
                                                    .append(str)
                                                    .append("\n ").append(addBlueColor("str(" + locale2 + "): "))
                                                    .append(str2).append("\n");
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
                    boolean appNameMatched = false;
                    String label = (String) context.getPackageManager().getApplicationLabel(pi.applicationInfo);
                    String label2 = "";
                    if (pi.applicationInfo.labelRes != 0 && resource2 != null) {
                        label2 = resource2.getString(pi.applicationInfo.labelRes);
                        if (label2.contains(mStr)
                                || label2.toLowerCase().contains(mStr.toLowerCase())) {
                            appNameMatched = true;
                        }
                    }
                    if (label.contains(mStr)
                            || label.toLowerCase().contains(mStr.toLowerCase())) {
                        appNameMatched = true;
                    }

                    if (appNameMatched) {
                        Log.e(TAG, "+++++ package: " + pi.packageName);
                        foundCnt++;
                        stringBuilder.append("\n--------------------\n ")
                                .append(addBlueColor(foundCnt + ". package: ")).append(pi.packageName)
                                .append("\n ").append(addBlueColor("app name (" + locale + "): "))
                                .append(label)
                                .append("\n ").append(addBlueColor("app name (" + locale2 + "): "))
                                .append(label2).append("\n");
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
        int finalFoundCnt1 = foundCnt;
        runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                "Done. Found " + finalFoundCnt1 + " matches.", Toast.LENGTH_LONG).show());
        Log.e(TAG, "Done!!!!!!");
        if (foundCnt <= 0) {
            stringBuilder.append("\nNot found.");
        } else {
            stringBuilder.insert(0, "Found " + foundCnt + " matches.\n");
        }

        String resultStr = stringBuilder.toString();
        FileUtils.saveFile(CheckStringsInAppsActivity.this,
                "found_result.txt", resultStr);

        resultStr = resultStr.replaceAll("\n", "<br>\n");
        resultStr = addBlueColor(resultStr, mStr);

        resultStr = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "    <head>\n" +
                "        <meta charset=\"utf-8\">\n" +
                "    </head>\n" +
                "    <body>" + resultStr +
                "   </body>\n" +
                "</html>";
        String finalResultStr = resultStr;
        runOnUiThread(() -> mBinding.result.setText(Html.fromHtml(finalResultStr, 0)));
        boolean htmlOk = FileUtils.saveFile(CheckStringsInAppsActivity.this,
                "found_result.html", resultStr);
        if (htmlOk) {
            runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                    "found_result.html is saved to Download dir.", Toast.LENGTH_LONG).show());
        }
    }

    void findStringsBruteForce() {
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

        int limitCount = 1000;
        String limitCountStr = mBinding.limitCount.getText().toString();
        if (!TextUtils.isEmpty(limitCountStr.trim())) {
            limitCount = Integer.parseInt(limitCountStr.trim());
            if (limitCount <= 0) {
                limitCount = 1000;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        int foundCnt = 0;
        if (filteredAppList.size() > 0) {
            int checkedPkgCnt = 0;
            int totalPkgCnt = filteredAppList.size();
            runOnUiThread(() -> mBinding.progress.setText("0/" + totalPkgCnt));
            stringBuilder.append("Check ").append(filteredAppList.size()).append(" apps.");
            Log.e(TAG, "Check " + filteredAppList.size() + " apps");
            for (PackageInfo pi : filteredAppList) {
                String pkgName = pi.packageName;
                Log.e(TAG, "======================== " + pkgName + " ========================");

                try {
                    Context context = createPackageContext(pkgName,
                            CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
                    Resources resources = context.getResources();
                    LocaleList localeList = resources.getConfiguration().getLocales();
                    Locale locale = localeList.get(0);

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

                    Locale locale2 = configuration.getLocales().get(0);
                    if (needCheckRes2) {
                        Context context2 = context.createConfigurationContext(configuration);
                        resource2 = context2.getResources();
                    }

                    ArrayList<Integer> stringStartIdList = new ArrayList<>();
                    for (int id = 0x7f010000; id < 0x7f150000; id += 0x10000) {
                        try {
                            TypedValue typedValue = new TypedValue();
                            resources.getValue(id, typedValue, true);
                            if (typedValue.type != TypedValue.TYPE_STRING) {
                                continue;
                            }
                            stringStartIdList.add(id);
                            Log.e(TAG, "Start string id: " + id + ", (hex): 0x" + Integer.toHexString(id));
                        } catch (Exception e) {
                            Log.e(TAG, e.getCause() + ": " + e.getMessage());
                        }
                    }
                    if (stringStartIdList.size() <= 0) {
                        Log.e(TAG, "No strings in pkg " + pi.packageName);
                        checkedPkgCnt++;
                        continue;
                    }
                    for (Integer startStrid : stringStartIdList) {
                        int failedCnt = 0;
                        for (int id = startStrid; id < startStrid + 0x10000; id++) {
                            if (showDebugLog)
                                Log.e(TAG, "id (hex): " + Integer.toHexString(id) + ", id: " + id);
                            try {
                                boolean matched = false;
                                String str = resources.getString(id);
                                if (showDebugLog) Log.e(TAG, "str: " + str);
                                if (str.contains(mStr)
                                        || str.toLowerCase().contains(mStr.toLowerCase())) {
                                    matched = true;
                                }

                                String str2 = "";
                                if (needCheckRes2) {
                                    str2 = resource2.getString(id);
                                    if (showDebugLog) Log.e(TAG, "str2: " + str2);
                                    if (str2.contains(mStr)
                                            || str2.toLowerCase().contains(mStr.toLowerCase())) {
                                        matched = true;
                                    }
                                }
                                if (matched) {
                                    Log.e(TAG, "+++++ package: " + pi.packageName);
                                    foundCnt++;
                                    stringBuilder.append("\n--------------------\n ")
                                            .append(addBlueColor(foundCnt + ". package: ")).append(pi.packageName)
                                            .append("\n ").append(addBlueColor("id(hex): "))
                                            .append("0x").append(Integer.toHexString(id))
                                            .append("\n ").append(addBlueColor("id(dec): ")).append(id)
                                            .append("\n ").append(addBlueColor("str(" + locale + "): "))
                                            .append(str)
                                            .append("\n ").append(addBlueColor("str(" + locale2 + "): "))
                                            .append(str2).append("\n");
                                }
                            } catch (Resources.NotFoundException e) {
                                Log.e(TAG, "Resources.NotFoundException: " + e.getMessage());
                                // when the string does not exists, then break this loop, try another string start id
                                failedCnt++;
                            } catch (RuntimeException e) {
                                Log.e(TAG, e.getMessage());
                                failedCnt++;
                            }
                            if (failedCnt >= limitCount) {
                                // sometimes some string ids are missing, skip them, but stop when exceed limit
                                break;
                            }
                        }
                    }

                    // Check application name
                    boolean appNameMatched = false;
                    String label = (String) context.getPackageManager().getApplicationLabel(pi.applicationInfo);
                    String label2 = "";
                    if (pi.applicationInfo.labelRes != 0 && resource2 != null) {
                        label2 = resource2.getString(pi.applicationInfo.labelRes);
                    }

                    if (label.contains(mStr)
                            || label.toLowerCase().contains(mStr.toLowerCase())) {
                        appNameMatched = true;
                    }
                    if (label2.contains(mStr)
                            || label2.toLowerCase().contains(mStr.toLowerCase())) {
                        appNameMatched = true;
                    }
                    if (appNameMatched) {
                        Log.e(TAG, "+++++ package: " + pi.packageName);
                        foundCnt++;
                        stringBuilder.append("\n--------------------\n ")
                                .append(addBlueColor(foundCnt + ". package: ")).append(pi.packageName)
                                .append("\n ").append(addBlueColor("app name (" + locale + "): "))
                                .append(label)
                                .append("\n ").append(addBlueColor("app name (" + locale2 + "): "))
                                .append(label2).append("\n");
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException: " + e.getMessage());
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "NameNotFoundException: " + e.getMessage());
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException: " + e.getMessage());
                }
                checkedPkgCnt++;
                int finalCheckedPkgCnt = checkedPkgCnt;
                runOnUiThread(() -> mBinding.progress.setText(finalCheckedPkgCnt + "/" + totalPkgCnt));
            }
        }
        int finalFoundCnt = foundCnt;
        runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                "Done. Found " + finalFoundCnt + " matches.", Toast.LENGTH_LONG).show());
        Log.e(TAG, "Done!!!!!!");
        if (foundCnt <= 0) {
            stringBuilder.append("\nNot found.");
        } else {
            stringBuilder.insert(0, "Found " + foundCnt + " matches.\n");
        }
        String resultStr = stringBuilder.toString();
        FileUtils.saveFile(CheckStringsInAppsActivity.this,
                "found_result.txt", resultStr);

        resultStr = resultStr.replaceAll("\n", "<br>\n");
        resultStr = addBlueColor(resultStr, mStr);
        resultStr = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "    <head>\n" +
                "        <meta charset=\"utf-8\">\n" +
                "    </head>\n" +
                "    <body>" + resultStr +
                "   </body>\n" +
                "</html>";

        String finalResultStr = resultStr;
        runOnUiThread(() -> mBinding.result.setText(Html.fromHtml(finalResultStr, 0)));
        boolean ok = FileUtils.saveFile(CheckStringsInAppsActivity.this,
                "found_result.html", resultStr);
        if (ok) {
            runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                    "Results are saved to Download dir.", Toast.LENGTH_LONG).show());
        }
    }

    String addBlueColor(String str, String word) {
        if (TextUtils.isEmpty(word)) {
            return str;
        }
        return str.replaceAll("(?i)(" + word + ")", "<font color=#0000ff>$1</font>");
    }

    String addBlueColor(String str) {
        return "<font color=#0000ff>" + str + "</font>";
    }

    private static final int REQUEST_PERMISSION = 100;

    private void requestRuntimePermissions() {
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        for (String perm : permissions) {
            requestOnePermission(perm);
        }
    }

    private void requestOnePermission(String permission) {
        if (ContextCompat.checkSelfPermission(CheckStringsInAppsActivity.this,
                permission) == PackageManager.PERMISSION_GRANTED) {
            // do nothing
        } else if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Without this permission, some functions will not work.")
                    .setPositiveButton("Reject", (dialog, which) -> dialog.dismiss())
                    .setNegativeButton("OK", (dialog, which) -> requestPermissions(
                            new String[]{permission},
                            REQUEST_PERMISSION)).setTitle("Need permission: " + permission)
                    .create().show();
        } else {
            requestPermissions(new String[]{permission}, REQUEST_PERMISSION);
        }
    }
}
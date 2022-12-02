package com.galian.samples;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
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
    private static final int DEFAULT_FAILED_COUNT_LIMIT = 100;
    private int FoundCnt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCheckStringsInAppsBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initViews();
        updateTips();
    }

    private void updateTips() {
        StringBuilder stringBuilder = new StringBuilder();
        if (mBinding.findByBruteForce.isChecked()) {
            stringBuilder.append("Brute force mode (traverse str ids); ");
        } else {
            stringBuilder.append("Normal mode (only strs in R.java); ");
        }

        if (mBinding.showMoreLog.isChecked()) {
            stringBuilder.append("More log; ");
        } else {
            stringBuilder.append("Less log; ");
        }
        if (mBinding.showExceptionLog.isChecked()) {
            stringBuilder.append("Include exception log; ");
        } else {
            stringBuilder.append("No exception log; ");
        }

        if (mBinding.systemApps.isChecked() && mBinding.nonSystemApps.isChecked()) {
            stringBuilder.append("Search all apps (sys & non-sys apps)");
        } else if (mBinding.systemApps.isChecked()) {
            stringBuilder.append("Search system apps only");
        } else if (mBinding.nonSystemApps.isChecked()) {
            stringBuilder.append("Search non-system apps only");
        } else {
            stringBuilder.append("At least 'system' or 'non-system' should be checked");
        }
        mBinding.tips.setText(stringBuilder.toString());
    }

    private void initViews() {
        mBinding.checkStrings.setOnClickListener(v -> checkStrings());
        mBinding.systemApps.setOnCheckedChangeListener((v, isChecked) -> updateTips());
        mBinding.nonSystemApps.setOnCheckedChangeListener((v, isChecked) -> updateTips());
        mBinding.showMoreLog.setOnCheckedChangeListener((v, isChecked) -> updateTips());
        mBinding.showExceptionLog.setOnCheckedChangeListener((v, isChecked) -> updateTips());
        mBinding.findByBruteForce.setOnCheckedChangeListener((v, isChecked) -> updateTips());
        mBinding.searchedStr.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                checkStrings();
                return true;
            }
            return false;
        });
        mBinding.foundCnt.setText("");
    }

    void checkStrings() {
        mStr = mBinding.searchedStr.getText().toString();
        if (TextUtils.isEmpty(mStr)) {
            Toast.makeText(this, "Please input string!!", Toast.LENGTH_LONG).show();
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

    static class ResConfig {
        Resources resources;
        Locale locale;

        public ResConfig(Resources r, Locale l) {
            resources = r;
            locale = l;
        }
    }

    ResConfig getResources2(Context pkgContext) {
        Resources resources = pkgContext.getResources();
        LocaleList localeList = resources.getConfiguration().getLocales();
        Locale locale = localeList.get(0);

        Resources resource2;
        Configuration configuration = new Configuration();
        if (locale.getLanguage().equals(Locale.CHINESE.getLanguage())) {
            configuration.setLocale(Locale.ENGLISH);
        } else if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            configuration.setLocale(Locale.CHINESE);
        } else {
            return null;
        }

        Locale locale2 = configuration.getLocales().get(0);
        Context context2 = pkgContext.createConfigurationContext(configuration);
        resource2 = context2.getResources();
        return new ResConfig(resource2, locale2);
    }

    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_ONLY_SYS_APP = 2;
    private static final int SCOPE_ONLY_NON_SYS_APP = 3;

    ArrayList<String> getRClassNames(ClassLoader pathClassLoader) {
        boolean showDebugLog = mBinding.showMoreLog.isChecked();
        boolean showExceptionLog = mBinding.showExceptionLog.isChecked();
        ArrayList<String> rClassNameList = new ArrayList<>();
        try {
            Class<?> baseDexClassLoaderCls = Class.forName("dalvik.system.BaseDexClassLoader");
            @SuppressLint("DiscouragedPrivateApi") Field pathListField = baseDexClassLoaderCls.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object dexPathListObj = pathListField.get(pathClassLoader);

            if (dexPathListObj == null) {
                return rClassNameList;
            }
            Class<?> dexPathListCls = dexPathListObj.getClass();
            Field dexElementsField = dexPathListCls.getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object elementArrayObj = dexElementsField.get(dexPathListObj);
            if (elementArrayObj == null) {
                return rClassNameList;
            }
            int len = Array.getLength(elementArrayObj);

            if (showDebugLog)
                Log.d(TAG, "-----------------------------------------------------");

            for (int i = 0; i < len; i++) {
                Object elementObj = Array.get(elementArrayObj, i);
                if (elementObj == null) {
                    continue;
                }
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
                        if (className.contains(".R$string")) {
                            if (showDebugLog) Log.d(TAG, className);
                            rClassNameList.add(className);
                        }
                    }
                }
            }
            if (showDebugLog)
                Log.d(TAG, "-----------------------------------------------------");
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException
                | NoSuchMethodException | InvocationTargetException e) {
            if (showExceptionLog) e.printStackTrace();
        }
        return rClassNameList;
    }

    String[] mNneedColorText = new String[]{
            "[0-9]+\\. Package: ", "App Name: ", "App Name \\([a-zA-Z_-]+\\): ",
            "Version Name: ", "Version Code: ",
            "Id\\(hex\\): ", "Id\\(dec\\): ", "Str\\([a-zA-Z_-]+\\): "
    };

    @SuppressLint("SetTextI18n")
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
        boolean showExceptionLog = mBinding.showExceptionLog.isChecked();
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

        StringBuilder stringBuilder = new StringBuilder();

        FoundCnt = 0;
        if (filteredAppList.size() > 0) {
            int checkedPkgCnt = 0;
            int totalPkgCnt = filteredAppList.size();
            int finalFoundCnt = FoundCnt;
            runOnUiThread(() -> mBinding.foundCnt.setText("Found " + finalFoundCnt));
            runOnUiThread(() -> mBinding.progress.setText("0/" + totalPkgCnt));
            stringBuilder.append("Check ").append(filteredAppList.size()).append(" apps");
            Log.d(TAG, "Check " + filteredAppList.size() + " apps");
            for (PackageInfo pi : filteredAppList) {
                String pkgName = pi.packageName;
                String versionName = pi.versionName;
                Long longVerCode = pi.getLongVersionCode();
                CharSequence appName = getPackageManager().getApplicationLabel(pi.applicationInfo);
                if (showDebugLog) Log.d(TAG, "===== checking pkg: " + pkgName);

                try {
                    Context context = createPackageContext(pkgName, CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
                    ClassLoader pathClassLoader;
                    try {
                        pathClassLoader = context.getClassLoader();
                    } catch (Throwable t) {
                        if (showExceptionLog) {
                            Log.e(TAG, "context.getClassLoader(): "
                                    + t.getClass().getCanonicalName() + ":" + t.getMessage());
                            t.printStackTrace();
                        }
                        checkedPkgCnt++;
                        int finalCheckedPkgCnt = checkedPkgCnt;
                        runOnUiThread(() -> mBinding.progress.setText(finalCheckedPkgCnt + "/" + totalPkgCnt));
                        continue;
                    }

                    ArrayList<String> rClassNameList = getRClassNames(pathClassLoader);
                    Resources resources = context.getResources();
                    LocaleList localeList = resources.getConfiguration().getLocales();
                    Locale locale = localeList.get(0);

                    Resources resource2 = null;
                    Locale locale2 = null;
                    ResConfig resConfig = getResources2(context);
                    if (resConfig != null) {
                        resource2 = resConfig.resources;
                        locale2 = resConfig.locale;
                    }

                    if (rClassNameList.size() > 0) {
                        HashSet<Integer> idPool = new HashSet<>();
                        for (String rClassName : rClassNameList) {
                            if (rClassName.equals("android.support.v7.appcompat.R$string")) {
                                continue;
                            }
                            if (showDebugLog) Log.d(TAG, "R class: " + rClassName);
                            Class<?> rClassObj = context.getClassLoader().loadClass(rClassName);
                            List<Integer> ids = ReflectUtils.getAllStaticFinalIntValues(rClassObj);
                            if (ids.size() > 0) {
                                for (Integer id : ids) {
                                    if (idPool.contains(id)) {
                                        continue;
                                    } else {
                                        idPool.add(id);
                                    }
                                    try {
                                        boolean matched = false;
                                        String str = resources.getString(id);
                                        if (showDebugLog) {
                                            Log.d(TAG, "id (hex): " + Integer.toHexString(id) + ", id: " + id);
                                            Log.d(TAG, "str : " + str);
                                        }
                                        if (str.contains(mStr)
                                                || str.toLowerCase().contains(mStr.toLowerCase())) {
                                            matched = true;
                                        }

                                        String str2 = "";
                                        if (resource2 != null) {
                                            str2 = resource2.getString(id);
                                            if (showDebugLog) {
                                                Log.d(TAG, "str2: " + str2);
                                            }
                                            if (str2.contains(mStr)
                                                    || str2.toLowerCase().contains(mStr.toLowerCase())) {
                                                matched = true;
                                            }
                                        }
                                        if (matched) {
                                            FoundCnt++;
                                            int finalFoundCnt2 = FoundCnt;
                                            runOnUiThread(() -> mBinding.foundCnt.setText("Found " + finalFoundCnt2));
                                            String resultLog = formatFoundResult(pi.packageName, appName,
                                                    versionName, longVerCode, id, locale, str,
                                                    resource2, locale2, str2);
                                            Log.d(TAG, resultLog);
                                            stringBuilder.append(resultLog);
                                        }
                                    } catch (Exception e) {
                                        if (showExceptionLog) {
                                            Log.e(TAG, "get string in findStringsInRClass(): "
                                                    + e.getClass().getCanonicalName() + ", " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } else {
                                if (showDebugLog) Log.d(TAG, "No ids.");
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
                        FoundCnt++;
                        int finalFoundCnt2 = FoundCnt;
                        runOnUiThread(() -> mBinding.foundCnt.setText("Found " + finalFoundCnt2));
                        String resultLog = formatAppNameResult(pi.packageName, locale, label, locale2, label2);
                        Log.d(TAG, resultLog);
                        stringBuilder.append(resultLog);
                    }
                } catch (SecurityException e) {
                    if (showExceptionLog) {
                        Log.e(TAG, e.getClass().getCanonicalName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    if (showExceptionLog) {
                        Log.e(TAG, "PackageManager.NameNotFoundException: " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    if (showExceptionLog) {
                        Log.e(TAG, "ClassNotFoundException: " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (RuntimeException e) {
                    if (showExceptionLog) {
                        Log.e(TAG, "RuntimeException: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                checkedPkgCnt++;
                int finalCheckedPkgCnt = checkedPkgCnt;
                runOnUiThread(() -> mBinding.progress.setText(finalCheckedPkgCnt + "/" + totalPkgCnt));
            }
        }

        String sysResult = findSystemStringsInR();
        stringBuilder.append(sysResult);

        int finalFoundCnt = FoundCnt;
        runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                "Done. Found " + finalFoundCnt + " matches.", Toast.LENGTH_LONG).show());
        Log.d(TAG, "Done!!!!!!");
        Log.d(TAG, "Found " + FoundCnt + " matches.");
        if (FoundCnt <= 0) {
            stringBuilder.append("\nNot found.");
        } else {
            stringBuilder.insert(0, "Found " + FoundCnt + " matches.\n");
        }

        String resultStr = stringBuilder.toString();
        if (FoundCnt > 0) {
            FileUtils.saveFile(CheckStringsInAppsActivity.this,
                    "found_result.txt", resultStr);
        }
        resultStr = resultStr.replaceAll("\n", "<br>\n");
        for (String str : mNneedColorText) {
            resultStr = Utils.Companion.addBlueColor(resultStr, str);
        }
        resultStr = Utils.Companion.addRedColor(resultStr, mStr);

        resultStr = wrapHtml(resultStr);
        String finalResultStr = resultStr;
        runOnUiThread(() -> mBinding.result.setText(Html.fromHtml(finalResultStr, 0)));
        if (FoundCnt > 0) {
            boolean htmlOk = FileUtils.saveFile(CheckStringsInAppsActivity.this,
                    "found_result.html", resultStr);
            if (htmlOk) {
                runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                        "found_result.html is saved to Download dir.", Toast.LENGTH_LONG).show());
            }
        }
    }

    @SuppressLint("SetTextI18n")
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
        boolean showExceptionLog = mBinding.showExceptionLog.isChecked();
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

        int limitCount = DEFAULT_FAILED_COUNT_LIMIT;
        String limitCountStr = mBinding.limitCount.getText().toString();
        if (!TextUtils.isEmpty(limitCountStr.trim())) {
            limitCount = Integer.parseInt(limitCountStr.trim());
            if (limitCount <= 0) {
                limitCount = DEFAULT_FAILED_COUNT_LIMIT;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        FoundCnt = 0;
        if (filteredAppList.size() > 0) {
            int checkedPkgCnt = 0;
            int totalPkgCnt = filteredAppList.size();
            int finalFoundCnt = FoundCnt;
            runOnUiThread(() -> mBinding.foundCnt.setText("Found " + finalFoundCnt));
            runOnUiThread(() -> mBinding.progress.setText("0/" + totalPkgCnt));
            stringBuilder.append("Check ").append(filteredAppList.size()).append(" apps.");
            Log.d(TAG, "Check " + filteredAppList.size() + " apps");

            for (PackageInfo pi : filteredAppList) {
                String pkgName = pi.packageName;
                String versionName = pi.versionName;
                Long longVerCode = pi.getLongVersionCode();
                CharSequence appName = getPackageManager().getApplicationLabel(pi.applicationInfo);
                if (showDebugLog) Log.d(TAG, "===== checking pkg: " + pkgName);

                try {
                    Context context = createPackageContext(pkgName,
                            CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
                    Resources resources = context.getResources();
                    LocaleList localeList = resources.getConfiguration().getLocales();
                    Locale locale = localeList.get(0);

                    Resources resource2 = null;
                    Locale locale2 = null;
                    ResConfig resConfig = getResources2(context);
                    if (resConfig != null) {
                        resource2 = resConfig.resources;
                        locale2 = resConfig.locale;
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
                            if (showDebugLog)
                                Log.d(TAG, "Start string id: " + id + ", (hex): 0x" + Integer.toHexString(id));
                        } catch (Exception e) {
                            if (showExceptionLog) {
                                Log.e(TAG, "resources.getValue(): " + e.getClass().getCanonicalName()
                                        + "" + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                    if (stringStartIdList.size() <= 0) {
                        if (showDebugLog) Log.d(TAG, "No strings in pkg " + pi.packageName);
                        checkedPkgCnt++;
                        continue;
                    }
                    for (Integer startStrId : stringStartIdList) {
                        int failedCnt = 0;
                        for (int id = startStrId; id < startStrId + 0x10000; id++) {
                            try {
                                boolean matched = false;
                                String str = resources.getString(id);
                                if (showDebugLog) {
                                    Log.d(TAG, "id (hex): " + Integer.toHexString(id) + ", id: " + id);
                                    Log.d(TAG, "str : " + str);
                                }
                                if (str.contains(mStr)
                                        || str.toLowerCase().contains(mStr.toLowerCase())) {
                                    matched = true;
                                }

                                String str2 = "";
                                if (resource2 != null) {
                                    str2 = resource2.getString(id);
                                    if (showDebugLog) {
                                        Log.d(TAG, "str2: " + str2);
                                    }
                                    if (str2.contains(mStr)
                                            || str2.toLowerCase().contains(mStr.toLowerCase())) {
                                        matched = true;
                                    }
                                }
                                if (matched) {
                                    FoundCnt++;
                                    int finalFoundCnt1 = FoundCnt;
                                    runOnUiThread(() -> mBinding.foundCnt.setText("Found " + finalFoundCnt1));
                                    String resultLog = formatFoundResult(pi.packageName, appName,
                                            versionName, longVerCode, id, locale, str,
                                            resource2, locale2, str2);
                                    Log.d(TAG, resultLog);
                                    stringBuilder.append(resultLog);
                                }
                            } catch (Resources.NotFoundException e) {
                                if (showExceptionLog) {
                                    Log.e(TAG, "in findStringsBruteForce(), Resources.NotFoundException: " + e.getMessage());
                                }
                                // when the string does not exists, then break this loop, try another string start id
                                failedCnt++;
                            } catch (RuntimeException e) {
                                if (showExceptionLog) {
                                    Log.e(TAG, e.getClass().getCanonicalName() + ": " + e.getMessage());
                                }
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
                    String label = "";
                    try {
                        label = (String) context.getPackageManager().getApplicationLabel(pi.applicationInfo);
                    } catch (Throwable e) {
                        if (showExceptionLog) {
                            Log.e(TAG, e.getClass().getCanonicalName() + ": " + e.getMessage());
                        }
                    }
                    String label2 = "";
                    if (pi.applicationInfo.labelRes != 0 && resource2 != null) {
                        try {
                            label2 = resource2.getString(pi.applicationInfo.labelRes);
                        } catch (Throwable e) {
                            if (showExceptionLog)
                                Log.e(TAG, e.getClass().getCanonicalName() + ": " + e.getMessage());
                        }
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
                        FoundCnt++;
                        int finalFoundCnt1 = FoundCnt;
                        runOnUiThread(() -> mBinding.foundCnt.setText("Found " + finalFoundCnt1));
                        String resultLog = formatAppNameResult(pi.packageName, locale, label, locale2, label2);
                        Log.d(TAG, resultLog);
                        stringBuilder.append(resultLog);
                    }
                } catch (SecurityException e) {
                    if (showExceptionLog) Log.e(TAG, "SecurityException: " + e.getMessage());
                } catch (PackageManager.NameNotFoundException e) {
                    if (showExceptionLog) Log.e(TAG, "NameNotFoundException: " + e.getMessage());
                } catch (RuntimeException e) {
                    if (showExceptionLog) Log.e(TAG, "RuntimeException: " + e.getMessage());
                }
                checkedPkgCnt++;
                int finalCheckedPkgCnt = checkedPkgCnt;
                runOnUiThread(() -> mBinding.progress.setText(finalCheckedPkgCnt + "/" + totalPkgCnt));
            }
        }

        String sysResult = findSystemStringsBruteForce();
        stringBuilder.append(sysResult);

        int finalFoundCnt = FoundCnt;
        runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                "Done. Found " + finalFoundCnt + " matches.", Toast.LENGTH_LONG).show());
        Log.d(TAG, "Done!!!!!!");
        Log.d(TAG, "Found " + FoundCnt + " matches.");
        if (FoundCnt <= 0) {
            stringBuilder.append("\nNot found.");
        } else {
            stringBuilder.insert(0, "Found " + FoundCnt + " matches.\n");
        }
        String resultStr = stringBuilder.toString();
        if (FoundCnt > 0) {
            FileUtils.saveFile(CheckStringsInAppsActivity.this,
                    "found_result.txt", resultStr);
        }

        resultStr = resultStr.replaceAll("\n", "<br>\n");
        for (String str : mNneedColorText) {
            resultStr = Utils.Companion.addBlueColor(resultStr, str);
        }
        resultStr = Utils.Companion.addRedColor(resultStr, mStr);
        resultStr = wrapHtml(resultStr);

        String finalResultStr = resultStr;
        runOnUiThread(() -> mBinding.result.setText(Html.fromHtml(finalResultStr, 0)));
        if (FoundCnt > 0) {
            boolean ok = FileUtils.saveFile(CheckStringsInAppsActivity.this,
                    "found_result.html", resultStr);
            if (ok) {
                runOnUiThread(() -> Toast.makeText(CheckStringsInAppsActivity.this,
                        "found_result.html is saved to Download dir.", Toast.LENGTH_LONG).show());
            }
        }
    }

    @SuppressLint("SetTextI18n")
    String findSystemStringsInR() {
        Log.d(TAG, "Checking package: android");
        boolean showDebugLog = mBinding.showMoreLog.isChecked();
        boolean showExceptionLog = mBinding.showExceptionLog.isChecked();
        String sysPackageName = "android";
        StringBuilder stringBuilder = new StringBuilder();
        try {
            Context sysContext = createPackageContext(sysPackageName,
                    CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
            ApplicationInfo appInfo = sysContext.getPackageManager().getApplicationInfo(sysPackageName, 0);
            CharSequence appName = sysContext.getPackageManager().getApplicationLabel(appInfo);
            PackageInfo sysPkgInfo = getPackageManager().getPackageInfo(sysPackageName, 0);
            String versionName = sysPkgInfo.versionName;
            long longVersionCode = sysPkgInfo.getLongVersionCode();

            Resources resources = sysContext.getResources();
            LocaleList localeList = resources.getConfiguration().getLocales();
            Locale locale = localeList.get(0);

            ResConfig resConfig = getResources2(sysContext);
            Resources resource2 = null;
            Locale locale2 = null;
            if (resConfig != null) {
                resource2 = resConfig.resources;
                locale2 = resConfig.locale;
            }
            ArrayList<String> rStringList = new ArrayList<>();
            rStringList.add("com.android.internal.R$string");
            //rStringList.add("android.support.v7.appcompat.R$string");
            //rStringList.add("android.support.v4.appcompat.R$string");
            rStringList.add("androidx.activity.R$string");
            rStringList.add("androidx.appcompat.R$string");
            rStringList.add("androidx.appcompat.resources.R$string");
            rStringList.add("androidx.asynclayoutinflater.R$string");
            rStringList.add("androidx.browser.R$string");
            rStringList.add("androidx.core.R$string");
            rStringList.add("androidx.constraintlayout.widget.R$string");
            rStringList.add("androidx.fragment.R$string");
            rStringList.add("androidx.legacy.coreutils.R$string");
            rStringList.add("androidx.legacy.coreui.R$string");

            HashSet<Integer> idPool = new HashSet<>();

            for (String rString : rStringList) {
                List<Integer> idList = ReflectUtils.getAllStaticIntValues(rString);
                if (showDebugLog) Log.d(TAG, "idList size: " + idList.size());
                for (Integer strId : idList) {
                    if (idPool.contains(strId)) {
                        continue;
                    }
                    idPool.add(strId);
                    boolean matched = false;
                    String str = sysContext.getResources().getString(strId);
                    if (showDebugLog) {
                        Log.d(TAG, "id: " + strId + ", hex(id): 0x" + Integer.toHexString(strId));
                        Log.d(TAG, "str : " + str);
                    }
                    if (str.contains(mStr)
                            || str.toLowerCase().contains(mStr.toLowerCase())) {
                        matched = true;
                    }

                    String str2 = "";
                    if (resource2 != null) {
                        str2 = resource2.getString(strId);
                        if (showDebugLog) Log.d(TAG, "str2: " + str2);
                        if (str2.contains(mStr)
                                || str2.toLowerCase().contains(mStr.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (matched) {
                        FoundCnt++;
                        int finalFoundCnt1 = FoundCnt;
                        runOnUiThread(() -> mBinding.foundCnt.setText("Found " + finalFoundCnt1));
                        String resultLog = formatFoundResult(sysPackageName, appName,
                                versionName, longVersionCode, strId, locale, str,
                                resource2, locale2, str2);
                        Log.d(TAG, resultLog);
                        stringBuilder.append(resultLog);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (showExceptionLog)
                Log.e(TAG, "in findSystemStringsInR() PackageManager.NameNotFoundException: " + e.getMessage());
        }
        return stringBuilder.toString();
    }

    @SuppressLint("SetTextI18n")
    String findSystemStringsBruteForce() {
        Log.d(TAG, "Checking package: android");
        boolean showDebugLog = mBinding.showMoreLog.isChecked();
        boolean showExceptionLog = mBinding.showExceptionLog.isChecked();
        String sysPackageName = "android";

        int limitCount = DEFAULT_FAILED_COUNT_LIMIT;
        String limitCountStr = mBinding.limitCount.getText().toString();
        if (!TextUtils.isEmpty(limitCountStr.trim())) {
            limitCount = Integer.parseInt(limitCountStr.trim());
            if (limitCount <= 0) {
                limitCount = DEFAULT_FAILED_COUNT_LIMIT;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        try {
            Context context = createPackageContext(sysPackageName,
                    CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(sysPackageName, 0);
            CharSequence appName = context.getPackageManager().getApplicationLabel(appInfo);
            PackageInfo sysPkgInfo = getPackageManager().getPackageInfo(sysPackageName, 0);
            String versionName = sysPkgInfo.versionName;
            long longVersionCode = sysPkgInfo.getLongVersionCode();

            Resources resources = context.getResources();
            LocaleList localeList = resources.getConfiguration().getLocales();
            Locale locale = localeList.get(0);

            Resources resource2 = null;
            Locale locale2 = null;
            ResConfig resConfig = getResources2(context);
            if (resConfig != null) {
                resource2 = resConfig.resources;
                locale2 = resConfig.locale;
            }

            ArrayList<Integer> stringStartIdList = new ArrayList<>();
            for (int id = 0x01010000; id < 0x01200000; id += 0x10000) {
                try {
                    TypedValue typedValue = new TypedValue();
                    resources.getValue(id, typedValue, true);
                    if (typedValue.type != TypedValue.TYPE_STRING) {
                        continue;
                    }
                    stringStartIdList.add(id);
                    if (showDebugLog)
                        Log.d(TAG, "Start string id: " + id + ", (hex): 0x" + Integer.toHexString(id));
                } catch (Exception e) {
                    if (showExceptionLog) {
                        if (e.getCause() != null) {
                            Log.e(TAG, "in findSystemStringsBruteForce() resources.getValue, " +
                                    e.getClass() + ", " + e.getCause() + ": " + e.getMessage());
                        } else {
                            Log.e(TAG, "in findSystemStringsBruteForce() resources.getValue, " +
                                    e.getClass() + ", " + e.getMessage());
                        }
                    }
                }
            }
            if (stringStartIdList.size() <= 0) {
                Log.d(TAG, "No strings in " + sysPackageName + ", something wrong.");
                return "";
            }
            for (Integer startStrId : stringStartIdList) {
                int failedCnt = 0;
                for (int id = startStrId; id < startStrId + 0x10000; id++) {
                    try {
                        boolean matched = false;
                        String str = resources.getString(id);
                        if (showDebugLog) {
                            Log.d(TAG, "id (hex): " + Integer.toHexString(id) + ", id: " + id);
                            Log.d(TAG, "str : " + str);
                        }
                        if (str.contains(mStr)
                                || str.toLowerCase().contains(mStr.toLowerCase())) {
                            matched = true;
                        }

                        String str2 = "";
                        if (resource2 != null) {
                            str2 = resource2.getString(id);
                            if (showDebugLog) {
                                Log.d(TAG, "str2: " + str2);
                            }
                            if (str2.contains(mStr)
                                    || str2.toLowerCase().contains(mStr.toLowerCase())) {
                                matched = true;
                            }
                        }
                        if (matched) {
                            FoundCnt++;
                            int finalFoundCnt1 = FoundCnt;
                            runOnUiThread(() -> mBinding.foundCnt.setText("Found " + finalFoundCnt1));
                            String resultLog = formatFoundResult(sysPackageName, appName,
                                    versionName, longVersionCode, id, locale, str,
                                    resource2, locale2, str2);
                            Log.d(TAG, resultLog);
                            stringBuilder.append(resultLog);
                        }
                    } catch (Resources.NotFoundException e) {
                        if (showExceptionLog)
                            Log.e(TAG, "Resources.NotFoundException: " + e.getMessage());
                        // when the string does not exists, then break this loop, try another string start id
                        failedCnt++;
                    } catch (RuntimeException e) {
                        if (showExceptionLog)
                            Log.e(TAG, e.getClass().getCanonicalName() + ": " + e.getMessage());
                        failedCnt++;
                    }
                    if (failedCnt >= limitCount) {
                        // sometimes some string ids are missing, skip them, but stop when exceed limit
                        break;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (showExceptionLog) {
                Log.e(TAG, "in findSystemStringsBruteForce() PackageManager.NameNotFoundException: " + e.getMessage());
            }
        }
        return stringBuilder.toString();
    }

    String formatFoundResult(String pkg, CharSequence appName, String versionName, Long longVerCode,
                             int id, Locale locale, String str,
                             Resources resource2, Locale locale2, String str2) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("\n--------------------\n");
        strBuilder.append(FoundCnt).append(". Package: ").append(pkg)
                .append("\nApp Name: ").append(appName)
                .append("\nVersion Name: ").append(versionName)
                .append("\nVersion Code: ").append(longVerCode)
                .append("\nId(hex): 0x").append(Integer.toHexString(id))
                .append("\nId(dec): ").append(id)
                .append("\nStr(").append(locale).append("): ").append(str);
        if (resource2 != null) {
            strBuilder.append("\nStr(").append(locale2).append("): ")
                    .append(str2);
        }
        return strBuilder.toString();
    }

    String formatAppNameResult(String pkg, Locale locale, String label,
                               Locale locale2, String label2) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("\n--------------------\n");
        strBuilder.append(FoundCnt).append(". Package: ").append(pkg)
                .append("\nApp Name (").append(locale).append("): ").append(label);
        if (!TextUtils.isEmpty(label2)) {
            strBuilder.append("\nApp Name (").append(locale2).append("): ").append(label2);
        }
        return strBuilder.toString();
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

    String wrapHtml(String str) {
        str = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "    <head>\n" +
                "        <meta charset=\"utf-8\">\n" +
                "    </head>\n" +
                "    <body>" + str +
                "   </body>\n" +
                "</html>";
        return str;
    }

    private static final int REQUEST_PERMISSION = 100;

    private void requestRuntimePermissions() {
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        requestPermissions(permissions, REQUEST_PERMISSION);
    }
}
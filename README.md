# Samples for test

## Protected Action Test

Check one action is protected or not.
Core code is as follows:

```java
public static boolean isProtectedBroatcast(String action) {
    Class<?> cls = null;
    Method method = null;
    boolean ret = false;

    try {
        cls = Class.forName("android.app.ActivityThread");
        method = cls.getMethod("getPackageManager", null);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        IPackageManager iPackageManager = (IPackageManager) method.invoke(cls, null);
        ret = iPackageManager.isProtectedBroadcast(action);
        Log.d(TAG, "action: " + action + " is " + (ret?"protected.":" not protected."));
    } catch (...) {
       ...
    }
    return ret;
}
```
framework-classes-full-debug.jar is needed and I compile it using android 8.0 code.
I already put framework-classes-full-debug.jar in this repo, address is
https://github.com/galian123/Samples/blob/master/mylib/libs/framework-classes-full-debug.jar

In build.gradle of mylib module, use framework-classes-full-debug.jar like this:
```groovy
dependencies {
    //compile fileTree(dir: 'libs', include: ['*.jar']) // should comment it, else method count will exceed 65536.
    provided files('libs/framework-classes-full-debug.jar')
    ...
}
```

## Test 'adb forward'

See `ServerActivity`

## Find strings in other apks

See `CheckStringsInAppsActivity`

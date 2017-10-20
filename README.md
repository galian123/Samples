# ProtectedBroadcastSample
判断一个action是否是受保护的
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
    } catch (ClassNotFoundException e) {
        e.printStackTrace();
    } catch (NoSuchMethodException e) {
        e.printStackTrace();
    } catch (InvocationTargetException e) {
        e.printStackTrace();
    } catch (IllegalAccessException e) {
        e.printStackTrace();
    } catch (RemoteException e) {
        e.printStackTrace();
    }
    return ret;
}
```
要想使上面的代码编译通过，还需要framework生成的jar包`framework-classes-full-debug.jar`。

我用Android 8.0 代码编译生成的 `framework-classes-full-debug.jar`，放到了github，地址为：https://github.com/galian123/ProtectedBroadcastSample/blob/master/mylib/libs/framework-classes-full-debug.jar。

在 build.gradle中引用 `framework-classes-full-debug.jar` 包：
```groovy
dependencies {
    //compile fileTree(dir: 'libs', include: ['*.jar']) // 一定要注释掉，否则会发生method数超过65536的问题
    provided files('libs/framework-classes-full-debug.jar')
    ...
}
```


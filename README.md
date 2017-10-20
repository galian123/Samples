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

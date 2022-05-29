package com.galian.mylib;

import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Created by galian on 2017/10/3.
 */

public class Utils {

    private static final String TAG = "Utils";
    public static boolean isProtectedBroatcast(String action) {
        Class<?> cls = null;
        Method method = null;
        boolean ret = false;

        try {
            cls = Class.forName("android.app.ActivityThread");
            method = cls.getMethod("getPackageManager");
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            IPackageManager iPackageManager = (IPackageManager) method.invoke(cls);
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
}

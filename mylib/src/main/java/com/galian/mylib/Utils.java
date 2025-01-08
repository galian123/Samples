package com.galian.mylib;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Created by galian on 2017/10/3.
 */

public class Utils {

    private static final String TAG = "Utils";

    public static int isProtectedBroadcast(String action) {
        int ret = -1;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.app.ActivityThread");
            @SuppressLint("DiscouragedPrivateApi") Method method = cls.getDeclaredMethod("getPackageManager");
            method.setAccessible(true);
            IInterface proxy = (IInterface) method.invoke(cls);
            if (proxy == null) {
                return -1;
            }
            IBinder binder = proxy.asBinder();

            data.writeInterfaceToken("android.content.pm.IPackageManager");
            data.writeString(action);

            int code = getTransCode("android.content.pm.IPackageManager$Stub", "TRANSACTION_isProtectedBroadcast");
            if (code != -1) {
                binder.transact(code, data, reply, 0);
                reply.readException();
                ret = reply.readInt();
            } else {
                Log.e(TAG, "Can not get the value of TRANSACTION_isProtectedBroadcast");
            }
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "NoSuchMethodException: " + e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
        if (ret != -1) {
            Log.d(TAG, action + ((ret == 1) ? " is protected." : " is not protected (or action not exists)."));
        } else {
            Log.d(TAG, "Can't determine whether action " + action + " is protected or not.");
        }
        return ret;
    }

    public static int getTransCode(String className, String transName) {
        try {
            Class<?> cls = Class.forName(className);
            Field field = cls.getDeclaredField(transName);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}

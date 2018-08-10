package com.galian.samples;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ReflectUtils {

    private static final String LOG_TAG = "ReflectUtils";
    private Class<?> mClassObj;

    public ReflectUtils() {
        mClassObj = null;
    }

    public ReflectUtils(Class<?> classObj) {
        mClassObj = classObj;
    }

    public ReflectUtils(String className) {
        setClassObj(className);
    }

    public boolean setClassObj(String className) {
        boolean result = false;
        Class<?> tempClass = null;

        try {
            tempClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (tempClass != null) {
            mClassObj = tempClass;
            result = true;
        } else {
            Log.e(LOG_TAG, "class not found.");
        }

        return result;
    }

    public List<Integer> getAllIntValues() {
        List<Integer> ids = new ArrayList<Integer>();
        Field[] fields = mClassObj.getDeclaredFields();
        if (fields != null && fields.length > 0) {

            for (Field field : fields) {
                String name = field.getName();
                //Log.d(LOG_TAG, "field name: " + name);
                try {
                    int val = field.getInt(null);
                    //Log.e(LOG_TAG, "field value(hex): " + Integer.toHexString(val));
                    ids.add(val);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoClassDefFoundError e) {
                    e.printStackTrace();
                } catch (NoSuchFieldError e) {
                    e.printStackTrace();
                }
            }
            return ids;
        } else {
            Log.d(LOG_TAG, "No fields.");
            return ids;
        }
    }

    private Field getField(String fieldName) {
        Field field = null;
        try {
            field = mClassObj.getDeclaredField(fieldName);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return field;
    }

    public int getStaticIntValue(String fieldName, int defaultValue) {
        if (mClassObj == null) {
            Log.d(LOG_TAG, "sClassObj is not set, return default value.");
            return defaultValue;
        }

        int result = defaultValue;
        Field field = getField(fieldName);
        if (field != null) {
            try {
                result = field.getInt(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        Log.d(LOG_TAG, "Field Name ( " + fieldName + " ), value = " + result);
        return result;
    }

    public Method getMethod(String methodName, Class<?>... parameterTypes) {
        Method method = null;
        try {
            method = mClassObj.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return method;
    }

    public Object invokeMethod(Method method, Object obj, Object... args) {
        Object retObj = null;
        try {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            if (obj == null) {
                obj = mClassObj;
            }
            retObj = method.invoke(obj, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return retObj;
    }
}

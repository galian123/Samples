package com.galian.samples;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class ReflectUtils {
    private static final String LOG_TAG = "ReflectUtils";

    public static List<Integer> getAllStaticIntValues(String className) {
        List<Integer> ids = new ArrayList<Integer>();
        try {
            Class<?> cls = Class.forName(className);
            Field[] fields = cls.getDeclaredFields();
            if (fields.length > 0) {
                for (Field field : fields) {
                    //String name = field.getName();
                    //Log.d(LOG_TAG, "field name: " + name);
                    field.setAccessible(true);
                    Class<?> type = field.getType();
                    if (type != int.class) {
                        continue;
                    }
                    int modifiers = field.getModifiers();
                    if (!Modifier.isStatic(modifiers)) {
                        continue;
                    }
                    try {
                        int val = field.getInt(null);
                        //Log.e(LOG_TAG, "field value(hex): " + Integer.toHexString(val));
                        ids.add(val);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }
                return ids;
            } else {
                Log.d(LOG_TAG, "No fields.");
                return ids;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return ids;
    }

    public static List<Integer> getAllStaticFinalIntValues(Class<?> cls) {
        List<Integer> ids = new ArrayList<Integer>();
        try {
            Field[] fields = cls.getDeclaredFields();
            if (fields.length > 0) {
                for (Field field : fields) {
                    //String name = field.getName();
                    //Log.d(LOG_TAG, "field name: " + name);
                    field.setAccessible(true);
                    Class<?> type = field.getType();
                    if (type != int.class) {
                        continue;
                    }
                    int modifiers = field.getModifiers();
                    if (!Modifier.isStatic(modifiers)
                            || !Modifier.isFinal(modifiers)) {
                        continue;
                    }
                    try {
                        int val = field.getInt(null);
                        //Log.e(LOG_TAG, "field value(hex): " + Integer.toHexString(val));
                        ids.add(val);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }
                return ids;
            } else {
                Log.d(LOG_TAG, "No fields.");
                return ids;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (Throwable t) {
            Log.e(LOG_TAG, t.getMessage());
        }
        return ids;
    }

    private Field getField(String className, String fieldName) {
        Field field = null;
        try {
            Class<?> cls = Class.forName(className);
            field = cls.getDeclaredField(fieldName);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return field;
    }

    public int getStaticIntValue(String className, String fieldName, int defaultValue) {
        int result = defaultValue;
        Field field = getField(className, fieldName);
        if (field != null) {
            if (!Modifier.isStatic(field.getModifiers())) {
                return defaultValue;
            }
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

    public Method getMethod(String className, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        try {
            Class<?> cls = Class.forName(className);
            method = cls.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return method;
    }

    public Object invokeStaticMethod(String className, String methodName, Class<?> parameterTypes, Object... args) {
        Object retObj = null;
        Method method;
        try {
            Class<?> cls = Class.forName(className);
            method = cls.getMethod(methodName, parameterTypes);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            retObj = method.invoke(null, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return retObj;
    }
}

package com.galian.samples;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.galian.mylib.Utils;

public class ProtectedActionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protected_action);
        String action = "android.intent.action.BOOT_COMPLETED";
        String action2 = "com.galian.action.MY_ACTION";
        Utils.isProtectedBroatcast(action);
        Utils.isProtectedBroatcast(action2);
    }
}

package com.galian.samples;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.galian.samples.databinding.ActivityMainBinding;

import org.lsposed.hiddenapibypass.HiddenApiBypass;


public class MainActivity extends AppCompatActivity {

    ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        HiddenApiBypass.addHiddenApiExemptions("");
        initViews();
    }

    private void initViews() {
        mBinding.testProtectedAction.setOnClickListener(v -> testProtectedAction());
        mBinding.testAdbForward.setOnClickListener(v -> testAdbForward());
        mBinding.testRxPermissionInActivity.setOnClickListener(v -> testRxPermissionInActivity());
        mBinding.testRxPermissionInFragment.setOnClickListener(v -> testRxPermissionInFragment());
        mBinding.checkStringsInApps.setOnClickListener(v -> checkStringsInApps());
    }

    void testProtectedAction() {
        Intent intent = new Intent(MainActivity.this, ProtectedActionActivity.class);
        startActivity(intent);
    }

    void testAdbForward() {
        Intent intent = new Intent(MainActivity.this, ServerActvitity.class);
        startActivity(intent);
    }

    void testRxPermissionInActivity() {
        Intent intent = new Intent(MainActivity.this, RxPermissionTestActivity.class);
        startActivity(intent);
    }

    void testRxPermissionInFragment() {
        Intent intent = new Intent(MainActivity.this, RxPermissionTestActivity2.class);
        startActivity(intent);
    }

    void checkStringsInApps() {
        Intent intent = new Intent(MainActivity.this, CheckStringsInAppsActivity.class);
        startActivity(intent);
    }
}

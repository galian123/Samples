package com.galian.samples;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.test_protected_action)
    void testProtectedAction() {
        Intent intent = new Intent(MainActivity.this, ProtectedActionActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.test_adb_forward)
    void testAdbForward() {
        Intent intent = new Intent(MainActivity.this, ServerActvitity.class);
        startActivity(intent);
    }

    @OnClick(R.id.test_rx_permission_in_activity)
    void testRxPermissionInActivity() {
        Intent intent = new Intent(MainActivity.this, RxPermissionTestActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.test_rx_permission_in_fragment)
    void testRxPermissionInFragment() {
        Intent intent = new Intent(MainActivity.this, RxPermissionTestActivity2.class);
        startActivity(intent);
    }
}

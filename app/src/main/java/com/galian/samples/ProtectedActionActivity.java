package com.galian.samples;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.galian.mylib.Utils;
import com.galian.samples.databinding.ActivityProtectedActionBinding;

public class ProtectedActionActivity extends AppCompatActivity {

    ActivityProtectedActionBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityProtectedActionBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        String action = "android.intent.action.BOOT_COMPLETED";
        String action2 = "com.galian.action.MY_ACTION";
        boolean actionProtected = Utils.isProtectedBroatcast(action);
        boolean action2Protected = Utils.isProtectedBroatcast(action2);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Test case 1: ");
        if (actionProtected) {
            stringBuilder.append("OK\nWe can know ").append(action)
                    .append(" is protected.");
        } else {
            stringBuilder.append("Failed\nWe don't know whether ").append(action)
                    .append(" is protected or not.");
        }
        stringBuilder.append("\n\nTest case 2: ");
        if (action2Protected) {
            stringBuilder.append("Failed\nWe know ").append(action2)
                    .append(" is not protected, but it is regarded as protected.");
        } else {
            stringBuilder.append("OK\nWe know ").append(action2)
                    .append(" is not protected.");
        }
        mBinding.textViewResult.setText(stringBuilder.toString());
    }
}

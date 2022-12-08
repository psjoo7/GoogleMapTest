package com.example.googlemaptest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.util.Log;
import android.content.Intent;


public class LoadingActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        startLoading();

    }

    //로딩화면 먼저 키고 딜레이 3초 주고 화면 종료 -> 메인액티비로 화면 전환
    private void startLoading() {
        Handler handle = new Handler();
        handle.postDelayed(new Runnable(){
            @Override
            public void run() {
                Intent intent = new Intent(LoadingActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        },3000);
    }
}
package com.example.subcode;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 需要将 RemoveAdsActivity 拷贝到自己工程 因为订阅信息需要根据包名来获取
                startActivity(new Intent(MainActivity.this, RemoveAdsActivity.class));
            }
        });
    }
}

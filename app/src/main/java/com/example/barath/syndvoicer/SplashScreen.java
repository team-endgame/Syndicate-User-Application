package com.example.barath.syndvoicer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class SplashScreen extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 4000;

    ImageView img ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        img = (ImageView) findViewById(R.id.logo);

        Animation animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in);
        img.startAnimation(animFadeIn);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent homeintent = new Intent(SplashScreen.this,PhoneNumber.class);
                startActivity(homeintent);
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                finish();

            }
        },SPLASH_TIME_OUT);
    }
}

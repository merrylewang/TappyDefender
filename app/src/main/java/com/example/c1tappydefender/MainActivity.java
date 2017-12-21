package com.example.c1tappydefender;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Here we set our UI layout as the view
        setContentView(R.layout.activity_main);
        // Get a reference to the button in our layout
        final Button buttonPlay =
                (Button)findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener(this);

        SharedPreferences prefs;
        SharedPreferences.Editor editor;
        prefs = getSharedPreferences("HiScores", MODE_PRIVATE);

        long fastestTime = prefs.getLong("fastestTime", 1000000);

        final TextView textFastestTime = (TextView)findViewById(R.id.textHighScore);

        textFastestTime.setText("Fastest Time:" + TDView.formatTime(fastestTime));

    }


    @Override
    public void onClick(View view) {
        Intent i = new Intent(this, GameActivity.class);
        startActivity(i);
        finish();
    }

    // If hits the back button, quit the app
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return false;
    }
}
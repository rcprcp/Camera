package com.cottagecoders.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Camera extends Activity {
    public static Context ctx;
    TextView tv;
    public static String TAG = "CameraApp";
    public final static String EXTENSION = ".jpg";
    public final static String PREFACE = "MyFile_";
    static String fileName;
    private long num = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Log.d(TAG, " camera onCreate -- got here.");

        //handy nickname.
        ctx = getApplicationContext();

        tv = (TextView) findViewById(R.id.status);
        tv.setTextSize((float) 16.0);


        Button start = (Button) findViewById(R.id.start);
        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                num = System.currentTimeMillis() / 1000;
                fileName = ctx.getFilesDir() + "/" + PREFACE + num + EXTENSION;
                Log.d(TAG, "the fileName: " + fileName);
                Intent intent = new Intent(ctx, NewTakePicture.class);
                intent.putExtra("name", fileName);
                intent.putExtra("size", 1024);
                Log.d(TAG, "start the camera intent...");
                startActivityForResult(intent, 234);
                tv.setText("");
            }
        });

        Button images = (Button) findViewById(R.id.images);
        images.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ctx, ListImages.class);
                startActivity(intent);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(): -- got here. requestCode "
                + requestCode + " resultCode " + resultCode);
        if (requestCode != 123) {

            if (data.getStringExtra("okpicture").equalsIgnoreCase("no")) {
                ((TextView) findViewById(R.id.status)).setText("ERROR - no image!");
            } else {
                Intent intent = new Intent(ctx, ShowImage.class);
                intent.putExtra("fileName", fileName);
                startActivityForResult(intent, 123);

            }
        } else {
        }

        return;
    }
}

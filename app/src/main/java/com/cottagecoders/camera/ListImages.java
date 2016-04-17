package com.cottagecoders.camera;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;

public class ListImages extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_images);

        TableLayout table = (TableLayout) findViewById(R.id.tab);

        //FileFilter
        File[] dirFiles = Camera.ctx.getFilesDir().listFiles();

        for (File f : dirFiles) {
            TextView tv = new TextView(getApplicationContext());
            Log.d(Camera.TAG, "Show: the file " + f.getAbsoluteFile());
            tv.setTextSize(16f);
            tv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.Black));
            tv.setText(f.getName());

            TableRow tr = new TableRow(getApplicationContext());

            tr.addView(tv);
            table.addView(tr);
        }
    }
}

package com.cottagecoders.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
            Log.d(Camera.TAG, "Show: the file " + f.getAbsoluteFile());

            TableRow tr = new TableRow(getApplicationContext());
            //TODO: create thumbnail here.
            TextView tv = new TextView(getApplicationContext());
            tv.setTextSize(16f);
            tv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.Black));
            tv.setText(f.getName());
            tr.addView(tv);

            Button b = new Button(getApplicationContext());
            //TODO: wire the button to delete the file.
            b.setText("Delete");
            b.setTag(f);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File f = (File)v.getTag();
                    Log.d(Camera.TAG, "deleting file name " + f.getName());
                    f.delete();
                }
            });


            tr.addView(b);
            table.addView(tr);
        }
    }
}

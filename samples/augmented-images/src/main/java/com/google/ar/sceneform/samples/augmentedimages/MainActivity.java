package com.google.ar.sceneform.samples.augmentedimages;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.android.filament.Engine;
import com.google.android.filament.filamat.MaterialBuilder;
import com.google.android.filament.filamat.MaterialPackage;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.EngineInstance;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.InstructionsController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {

    private Button augmentedActivity, view3dActivity, addDatabase;
    private ProgressDialog progressDialog;
    private DatabaseBuilder databaseBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Activity Starting");
        progressDialog.setProgress(10);
        progressDialog.setMax(100);
        progressDialog.setMessage("Loading...");

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 2);
        }

        augmentedActivity = findViewById(R.id.augmented);
        augmentedActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MyTask().execute();
                Intent myIntent = new Intent(MainActivity.this, AugmentedImages.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        view3dActivity = findViewById(R.id.view3d);
        view3dActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MyTask().execute();
                Intent myIntent = new Intent(MainActivity.this, View3DModelActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        findViewById(R.id.add_database).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MyTask().execute();
                Intent myIntent = new Intent(MainActivity.this, AddDatabase.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            ((ViewGroup.MarginLayoutParams) toolbar.getLayoutParams()).topMargin = insets
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                    .top;

            return WindowInsetsCompat.CONSUMED;
        });
    }

    public class MyTask extends AsyncTask<Void, Void, Void> {
        public void onPreExecute() {
            progressDialog.show();
        }
        public Void doInBackground(Void... unused) {
            return null;
        }
    }
}
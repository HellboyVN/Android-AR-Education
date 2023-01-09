package com.google.ar.sceneform.samples.augmentedimages;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class View3DModelActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener {

    private ArFragment arFragment;
    private Renderable model;
    private ViewRenderable viewRenderable;
    private String[] allModels;
    private Spinner dropdown;
    private String modelName;
    private HashMap<String,String> listOfModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view3d_model);
        dropdown = findViewById(R.id.allItems);
        listOfModels = new HashMap<>();
        try {
            String[] temps = getAssets().list("models");
            for (String temp:temps) {
                listOfModels.put(temp, "models/" + temp);
            }

            File jsonFile = new File(getExternalFilesDir(null) + "/maps.json");
            if (jsonFile.exists()) {
                JSONObject obj = new JSONObject(AddDatabase.getJsonFromFile(jsonFile));
                JSONArray jsonData = obj.getJSONArray("data");
                for (int i = 0; i < jsonData.length(); i++) {
                    JSONObject mJsonObj = jsonData.getJSONObject(i);
                    if (!mJsonObj.getString("model").toLowerCase().contains("mp4")) {
                        listOfModels.put(mJsonObj.getString("tag"), mJsonObj.getString("model"));
                    }

                }
            } else {
                Log.d("END", "JSON data NOT detected ");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        allModels = listOfModels.keySet().toArray(new String[0]);
        modelName = "models/tiger1.glb";

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, allModels);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                modelName = listOfModels.get(adapterView.getItemAtPosition(i).toString());
                loadModels(modelName);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });


        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }
//        loadModels("models/tiger1.glb");
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
            arFragment.setOnTapArPlaneListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
    }

    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);

        // Fine adjust the maximum frame rate
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(View3DModelActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void loadModels(String source) {
        WeakReference<View3DModelActivity> weakActivity = new WeakReference<>(this);
        ModelRenderable.builder()
                .setSource(this, Uri.parse(source))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(model -> {
                    View3DModelActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.model = model;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(
                            this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
        ViewRenderable.builder()
                .setView(this, R.layout.view_model_title)
                .build()
                .thenAccept(viewRenderable -> {
                    View3DModelActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.viewRenderable = viewRenderable;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {

        if (model == null || viewRenderable == null) {
            Toast.makeText(this, " Loading " + modelName, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the Anchor.
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.getScaleController().setMinScale(0.01f);
        model.getScaleController().setMaxScale(3.0f);
        Box boundingBox = (Box) model.getCollisionShape();
        if (boundingBox != null) {
            Vector3 boundingBoxSize = boundingBox.getSize();
            float maxExtent = Math.max(boundingBoxSize.x, Math.max(boundingBoxSize.y, boundingBoxSize.z));
            float targetSize = 0.2f; // Whatever size you want.
            float scale = targetSize / maxExtent;
            model.setLocalScale(Vector3.one().scaled(scale));
        }

        model.setParent(anchorNode);
        model.setRenderable(this.model)
                .animate(true).start();
        model.select();

        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setName(modelName.replace("models/", ""));
        titleNode.setEnabled(false);
        titleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
        TextView textView = viewRenderable.getView().findViewById(R.id.model_name);
        textView.setText(modelName.replace("models/", "").replace(".glb", ""));
        titleNode.setRenderable(viewRenderable);
        titleNode.setEnabled(true);
    }

}
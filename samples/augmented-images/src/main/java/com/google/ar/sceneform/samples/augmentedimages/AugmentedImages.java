package com.google.ar.sceneform.samples.augmentedimages;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class AugmentedImages extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener {

    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private ArFragment arFragment;
    private boolean isDetected = false;
    private Button reset;
    DatabaseBuilder databaseBuilder;
    private AugmentedImageDatabase database;
    private Renderable plainVideoModel;
    private Material plainVideoMaterial;
    private MediaPlayer mediaPlayer;
    private JSONArray jsonData;
    private SpeechRecognizer speechRecognizer;
    private TextView editText;
    private ImageView micButton;
    private TransformableNode selectedModel;
    private AnchorNode selectedAnchorNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_augmented_image);

        //Record audio
        editText = findViewById(R.id.textview);
        micButton = findViewById(R.id.speech);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(AugmentedImages.this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);


        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                editText.setText("");
                editText.setHint("Listening...");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }
            final float[] cnt = {1,1,1,1};
            @Override
            public void onResults(Bundle bundle) {
                micButton.setImageResource(R.drawable.ic_mic_black_off);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d("END", data.toString());
                String move = data.get(0);
                editText.setText(move);
                if (selectedModel != null) {
                    selectedModel.getRotationController().setEnabled(true);
                    selectedModel.getScaleController().setEnabled(true);
                    selectedModel.select();
                    Vector3 curr = selectedModel.getLocalPosition();
                    if (move.toUpperCase().contains("UP")) {
                        cnt[0]++;
                        selectedModel.setLocalRotation(Quaternion.axisAngle(new Vector3(cnt[0], 0, 0), 45f));
                    } else if (move.toUpperCase().contains("DOWN")) {
                        cnt[0]--;
                        selectedModel.setLocalRotation(Quaternion.axisAngle(new Vector3(cnt[0], 0, 0), -45f));
                    } else if (move.toUpperCase().contains("LEFT")) {
                        cnt[1]++;
                        selectedModel.setLocalRotation(Quaternion.axisAngle(new Vector3(0, cnt[1], 0), 45f));
                    } else if (move.toUpperCase().contains("RIGHT")) {
                        cnt[1]--;
                        selectedModel.setLocalRotation(Quaternion.axisAngle(new Vector3(0, cnt[1], 0), -45f));
                    } else if (move.toUpperCase().contains("IN")) {
                        Log.d("END", "IN");
                        selectedModel.setLocalScale(new Vector3(3.1f,3.1f,3.1f));
                    } else if (move.toUpperCase().contains("OUT")) {
                        Log.d("END", "OUT");
                        selectedModel.setLocalScale(new Vector3(0.1f,0.1f,0.1f));
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        micButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    speechRecognizer.stopListening();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    micButton.setImageResource(R.drawable.ic_mic_black_24dp);
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
                return false;
            }

        });

        File jsonFile = new File(getExternalFilesDir(null) + "/maps.json");
        if (jsonFile.exists()) {
            JSONObject obj = null;
            try {
                obj = new JSONObject(AddDatabase.getJsonFromFile(jsonFile));
                jsonData = obj.getJSONArray("data");
                Log.d("END", "JSON data detected " + jsonData.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("END", "JSON data NOT detected ");
        }

        reset = findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isDetected = false;
                if (selectedModel != null) {
                    selectedModel = null;
                }
                // remove all child nodes in arview
                List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
                for (Node node : children) {
                    if (node instanceof AnchorNode) {
                        if (((AnchorNode) node).getAnchor() != null) {
                            ((AnchorNode) node).getAnchor().detach();
                        }
                    }
                    if (!(node instanceof Camera)) {
                        node.setParent(null);
                    }
                }

                futures.forEach(future -> {
                    if (!future.isDone())
                        future.cancel(true);
                });

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                }
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

        if(Sceneform.isSupported(this)) {
            // .glb models can be loaded at runtime when needed or when app starts
            // This method loads ModelRenderable when app starts
            loadMatrixModel();
            loadMatrixMaterial();
        }
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        // Disable plane detection
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)
        databaseBuilder = new DatabaseBuilder(session, getApplicationContext());
        database = databaseBuilder.getDatabase();

        config.setAugmentedImageDatabase(database);

        // Check for image detection
        arFragment.setOnAugmentedImageUpdateListener(this::onAugmentedImageTrackingUpdate);
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(AugmentedImages.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
        futures.forEach(future -> {
            if (!future.isDone())
                future.cancel(true);
        });

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    private void modelMovement(TransformableNode node, String moveMent) {
        Vector3 currentPosition = new Vector3();
        Vector3 move = new Vector3();

        try {
            currentPosition = Objects.requireNonNull(node.getLocalPosition());

            if (moveMent.equals("down")) {
                move.set(currentPosition.x, (float) (currentPosition.y - 0.1), currentPosition.z);
//                localPosition = move;
            }

            if (moveMent.equals("up")) {

                move.set(currentPosition.x, (float) (currentPosition.y + 0.1), currentPosition.z);
//                localPosition = move;
            }

            if (moveMent.equals("right_move")) {

                move.set((float) (currentPosition.x + 0.1), currentPosition.y, currentPosition.z);
//                localPosition = move;
            }

            if (moveMent.equals("left_move")) {

                move.set((float) (currentPosition.x - 0.1), currentPosition.y, currentPosition.z);
//                localPosition = move;
            }
//            if (moveMent.equals("rotate_left")) {
//                localPosition = currentPosition;
//                rotateLeft(node, objAxis, currentPosition);
//            }
//
//            if (moveMent.equals("rotate_right")) {
//                localPosition = currentPosition;
//                rotateRight(node, objAxis, currentPosition);
//            }

            if (moveMent.equals("zoom_in")) {
                move.set(currentPosition.x, currentPosition.y, (float) (currentPosition.z + 1));
//                localPosition = move;
            }

            if (moveMent.equals("zoom_out")) {
                move.set(currentPosition.x, currentPosition.y, (float) (currentPosition.z - 1));
//                localPosition = move;
            }


            node.setLocalPosition(move);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMatrixModel() {
        futures.add(ModelRenderable.builder()
                .setSource(this, Uri.parse("models/Video.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(model -> {
                    //removing shadows for this Renderable
                    model.setShadowCaster(false);
                    model.setShadowReceiver(true);
                    plainVideoModel = model;
                })
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show();
                            return null;
                        }));
    }

    private void loadMatrixMaterial() {
        Engine filamentEngine = EngineInstance.getEngine().getFilamentEngine();

        MaterialBuilder.init();
        MaterialBuilder materialBuilder = new MaterialBuilder()
                .platform(MaterialBuilder.Platform.MOBILE)
                .name("External Video Material")
                .require(MaterialBuilder.VertexAttribute.UV0)
                .shading(MaterialBuilder.Shading.UNLIT)
                .doubleSided(true)
                .samplerParameter(MaterialBuilder.SamplerType.SAMPLER_EXTERNAL, MaterialBuilder.SamplerFormat.FLOAT, MaterialBuilder.ParameterPrecision.DEFAULT, "videoTexture")
                .optimization(MaterialBuilder.Optimization.NONE);

        MaterialPackage plainVideoMaterialPackage = materialBuilder
                .blending(MaterialBuilder.BlendingMode.OPAQUE)
                .material("void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor = texture(materialParams_videoTexture, getUV0()).rgba;\n" +
                        "}\n")
                .build(filamentEngine);
        if (plainVideoMaterialPackage.isValid()) {
            ByteBuffer buffer = plainVideoMaterialPackage.getBuffer();
            futures.add(Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept(material -> {
                        plainVideoMaterial = material;
                    })
                    .exceptionally(
                            throwable -> {
                                Toast.makeText(this, "Unable to load material", Toast.LENGTH_LONG).show();
                                return null;
                            }));
        }
        MaterialBuilder.shutdown();
    }

    public void onAugmentedImageTrackingUpdate(AugmentedImage augmentedImage) {
        // If all images are already detected, for better CPU usage we do not need scan for them
        if (isDetected) {
            return;
        }

        arFragment.getInstructionsController().setEnabled(
                InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, true);

        if (augmentedImage.getTrackingState() == TrackingState.TRACKING
                && augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {

            // Setting anchor to the center of Augmented Image
            AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));
            selectedAnchorNode = anchorNode;
            String modelToLoad = "";

            String tagDetected = augmentedImage.getName();
            Log.d("END", "Tag detected: " + tagDetected);
//            if (editText.getText().toString().toUpperCase().contains("DOG")) {
//                isDetected = true;
//                modelToLoad = "models/dog.glb";
//                loadModelOnDetectedImage(DatabaseBuilder.DOG_TAG, anchorNode, augmentedImage, modelToLoad);
//            }

            if (tagDetected.equals(DatabaseBuilder.HO_CHI_MINH_TAG)) {
                isDetected = true;
                loadVideoOnDetectedImage(DatabaseBuilder.HO_CHI_MINH_TAG, anchorNode, augmentedImage, R.raw.video);
            }

            if (tagDetected.equals(DatabaseBuilder.CHEMISTRY_TAG)) {
                isDetected = true;
                loadVideoOnDetectedImage(DatabaseBuilder.CHEMISTRY_TAG, anchorNode, augmentedImage, R.raw.chemistry);
            }

            if (tagDetected.equals(DatabaseBuilder.TIGER_TAG)) {
                isDetected = true;
                modelToLoad = "models/tiger1.glb";
                loadModelOnDetectedImage(DatabaseBuilder.TIGER_TAG, anchorNode, augmentedImage, modelToLoad);
            }

            if (tagDetected.equals(DatabaseBuilder.EARTH_TAG)) {
                isDetected = true;
                modelToLoad = "models/earth5.glb";
                loadModelOnDetectedImage(DatabaseBuilder.EARTH_TAG, anchorNode, augmentedImage, modelToLoad);
            }

            if (tagDetected.equals(DatabaseBuilder.CAT_TAG)) {
                isDetected = true;
                modelToLoad = "models/cat.glb";
                loadModelOnDetectedImage(DatabaseBuilder.CAT_TAG, anchorNode, augmentedImage, modelToLoad);
            }

            if (tagDetected.equals(DatabaseBuilder.CHICKEN_TAG)) {
                isDetected = true;
                modelToLoad = "models/chicken.glb";
                loadModelOnDetectedImage(DatabaseBuilder.CHICKEN_TAG, anchorNode, augmentedImage, modelToLoad);
            }

            if (tagDetected.equals(DatabaseBuilder.DOG_TAG)) {
                isDetected = true;
                modelToLoad = "models/dog.glb";
                loadModelOnDetectedImage(DatabaseBuilder.DOG_TAG, anchorNode, augmentedImage, modelToLoad);
            }

            if (tagDetected.equals(DatabaseBuilder.DUCK_TAG)) {
                isDetected = true;
                modelToLoad = "models/duck.glb";
                loadModelOnDetectedImage(DatabaseBuilder.DUCK_TAG, anchorNode, augmentedImage, modelToLoad);
            }

            if (tagDetected.equals(DatabaseBuilder.PIG_TAG)) {
                isDetected = true;
                modelToLoad = "models/pig.glb";
                loadModelOnDetectedImage(DatabaseBuilder.PIG_TAG, anchorNode, augmentedImage, modelToLoad);
            }

            if (tagDetected.equals(DatabaseBuilder.ELEPHANT_TAG)) {
                isDetected = true;
                modelToLoad = "models/elephant.glb";
                loadModelOnDetectedImage(DatabaseBuilder.ELEPHANT_TAG, anchorNode, augmentedImage, modelToLoad);
            }

            if (tagDetected.equals(DatabaseBuilder.MATH_TAG)) {
                isDetected = true;
                modelToLoad = "models/xyz.glb";
                loadModelOnDetectedImage(DatabaseBuilder.MATH_TAG, anchorNode, augmentedImage, modelToLoad);
            }


            if (jsonData != null) {

                Boolean isExist = false;
                for (int i = 0; i < jsonData.length(); i++) {
                    try {
                        JSONObject mJsonObj = jsonData.getJSONObject(i);
                        if (mJsonObj.getString("tag").equals(tagDetected)) {
                            modelToLoad = mJsonObj.getString("model");
                            int index = modelToLoad.indexOf(":");
                            if (index != -1) {
                                modelToLoad = modelToLoad.substring(index+1);
                            }
                            isExist = true;
                            break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("END", "Model To Load " + modelToLoad);
                if (isExist) {
                    isDetected = true;
                    if (modelToLoad.toLowerCase().contains("mp4")) {
                        loadVideoOnDetectedImage(tagDetected, anchorNode, augmentedImage, modelToLoad);
                    } else {
                        loadModelOnDetectedImage(tagDetected, anchorNode, augmentedImage, modelToLoad);
                    }

                }
            } else {
                Log.d("END", "HMMM, NO JSON ");
            }

        }

        if (isDetected) {
            Log.d("END", "stop scanning");
            arFragment.getInstructionsController().setEnabled(InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false);
        }
    }

    private void loadVideoOnDetectedImage(String imageName,AnchorNode anchorNode, AugmentedImage augmentedImage, int video) {
        Toast.makeText(this, imageName + " tag detected", Toast.LENGTH_SHORT).show();

        // AnchorNode placed to the detected tag and set it to the real size of the tag
        // This will cause deformation if your AR tag has different aspect ratio than your video
        anchorNode.setWorldScale(new Vector3(augmentedImage.getExtentX(), 1f, augmentedImage.getExtentZ()));
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        TransformableNode videoNode = new TransformableNode(arFragment.getTransformationSystem());
        // For some reason it is shown upside down so this will rotate it correctly
        videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0), 180f));
        videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0f, 0), 180f));
        anchorNode.addChild(videoNode);

        // Setting texture
        ExternalTexture externalTexture = new ExternalTexture();
        RenderableInstance renderableInstance = videoNode.setRenderable(plainVideoModel);
        renderableInstance.setMaterial(plainVideoMaterial);
        selectedModel = videoNode;

        // Setting MediaPLayer
        renderableInstance.getMaterial().setExternalTexture("videoTexture", externalTexture);

        mediaPlayer = MediaPlayer.create(this, video);
        mediaPlayer.setLooping(true);
        mediaPlayer.setSurface(externalTexture.getSurface());
        mediaPlayer.start();
    }

    private void loadVideoOnDetectedImage(String imageName,AnchorNode anchorNode, AugmentedImage augmentedImage, String video) {
        Toast.makeText(this, imageName + " tag detected", Toast.LENGTH_SHORT).show();

        // AnchorNode placed to the detected tag and set it to the real size of the tag
        // This will cause deformation if your AR tag has different aspect ratio than your video
        anchorNode.setWorldScale(new Vector3(augmentedImage.getExtentX(), 1f, augmentedImage.getExtentZ()));
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        TransformableNode videoNode = new TransformableNode(arFragment.getTransformationSystem());
        // For some reason it is shown upside down so this will rotate it correctly
        videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0), 180f));
        videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0f, 0), 180f));
        anchorNode.addChild(videoNode);

        // Setting texture
        ExternalTexture externalTexture = new ExternalTexture();
        RenderableInstance renderableInstance = videoNode.setRenderable(plainVideoModel);
        renderableInstance.setMaterial(plainVideoMaterial);
        selectedModel = videoNode;

        // Setting MediaPLayer
        renderableInstance.getMaterial().setExternalTexture("videoTexture", externalTexture);

        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(new File(video)));
        mediaPlayer.setLooping(true);
        mediaPlayer.setSurface(externalTexture.getSurface());
        mediaPlayer.start();
    }

    private void loadModelOnDetectedImage(String imageName,AnchorNode anchorNode, AugmentedImage augmentedImage, String model) {
        Toast.makeText(this, imageName + " tag detected", Toast.LENGTH_SHORT).show();
        anchorNode.setWorldScale(new Vector3(0.1f, 0.1f, 0.1f));

        arFragment.getArSceneView().getScene().addChild(anchorNode);
        Uri uri = Uri.parse(model);
        Log.e("END", "URI test = " + uri.toString() + " - " + model);
        if (model.contains("storage/")) {
            uri = Uri.fromFile(new File(model));
            File test = new File(model);
            Log.e("END2",uri.toString() + " - " + test.canRead() + " - " + test.getAbsolutePath());
        } else {
            Log.e("END3",uri.toString());
        }
        Log.e("END4", "URI test = " + uri.toString());

        futures.add(ModelRenderable.builder()
                .setSource(this, uri) //models/tiger1.glb
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(rabbitModel -> {
                    TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                    Box boundingBox = (Box) modelNode.getCollisionShape();
                    if (boundingBox != null) {
                        Vector3 boundingBoxSize = boundingBox.getSize();
                        float maxExtent = Math.max(boundingBoxSize.x, Math.max(boundingBoxSize.y, boundingBoxSize.z));
                        float targetSize = 0.5f; // Whatever size you want.
                        float scale = targetSize / maxExtent;
                        modelNode.setLocalScale(Vector3.one().scaled(scale));
                    }
                    modelNode.getScaleController().setMinScale(0.1f);
                    modelNode.getScaleController().setMaxScale(9.0f);
                    modelNode.setLocalScale(new Vector3(1f, 1f, 1f));
                    modelNode.setRenderable(rabbitModel).animate(true).start();
                    modelNode.setLocalRotation(Quaternion.axisAngle(new Vector3(1, 0, 0), -90f));
                    selectedModel = modelNode;
                    anchorNode.addChild(modelNode);
                })
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
                            return null;
                        }));
    }

}

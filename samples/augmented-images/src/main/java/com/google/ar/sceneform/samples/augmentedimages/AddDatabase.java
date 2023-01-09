package com.google.ar.sceneform.samples.augmentedimages;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AddDatabase extends AppCompatActivity implements FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener {

    private Button selectImage, selectModel, addToDatabase,removeDatabase;
    private DatabaseBuilder databaseBuilder;
    private AugmentedImageDatabase database;
    private TextView imagePath, modelPath;
    private ActivityResultLauncher<Intent> startActivityForResultImage, startActivityForResultModel;
    private Uri dataUri;
    private ArFragment arFragment;
    private ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_data);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            ((ViewGroup.MarginLayoutParams) toolbar.getLayoutParams()).topMargin = insets
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                    .top;

            return WindowInsetsCompat.CONSUMED;
        });

        if(ActivityCompat.checkSelfPermission(AddDatabase.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AddDatabase.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                } catch (Exception ex){
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }

        if(ActivityCompat.checkSelfPermission(AddDatabase.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AddDatabase.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);

        }

        imagePath = findViewById(R.id.image_path);
        modelPath = findViewById(R.id.model_path);
        imageView = findViewById(R.id.imageview);

        selectImage = findViewById(R.id.add_image);
        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResultImage.launch(Intent.createChooser(intent, "Select Picture"));
            }
        });

        addToDatabase = findViewById(R.id.add_to_database);
        addToDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tag = "";
                if (dataUri != null) {
                    try {
                        tag = imagePath.getText().toString();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), dataUri);
                        serialize(tag, bitmap);

                        File file = new File(getExternalFilesDir(null) + "/maps.json");
                        JSONObject mathModel = new JSONObject();
                        mathModel.put("tag", tag);
                        mathModel.put("model", modelPath.getText().toString());

                        if (file.exists()) {
                            JSONObject obj = new JSONObject(getJsonFromFile(file));
                            JSONArray jsonArray = obj.getJSONArray("data");
                            Boolean isExist = false;
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject mJsonObj = jsonArray.getJSONObject(i);
                                if (mJsonObj.getString("tag").equals(tag)) {
                                    mJsonObj.put("model",modelPath.getText().toString());
                                    isExist = true;
                                    break;
                                }
                            }

                            if (!isExist) {
                                jsonArray.put(mathModel);
                            }

                            JSONObject currentJsonObject = new JSONObject();
                            currentJsonObject.put("data",jsonArray);
                            writeJsonFile(file, currentJsonObject.toString());
                            Log.d("END", "Append JSON = " + currentJsonObject.toString());
                        } else {
                            JSONArray jsonArray = new JSONArray();
                            jsonArray.put(mathModel);
                            JSONObject currentJsonObject = new JSONObject();
                            currentJsonObject.put("data",jsonArray);
                            writeJsonFile(file, currentJsonObject.toString());
                            Log.d("END", " new created JSON = " + currentJsonObject.toString());
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        removeDatabase = findViewById(R.id.remove_database);
        removeDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(AddDatabase.this)
                        .setTitle("Delete all database")
                        .setMessage("Are you sure you want to delete all your database?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                                File file = new File(getExternalFilesDir(null) + "/dbTest.imgdb");
                                if (file.exists()) {
                                    file.delete();
                                    Log.d("END", "database removed");
                                } else {
                                    Log.d("END", "database was not exist");
                                }

                                File file1 = new File(getExternalFilesDir(null) + "/maps.json");
                                if (file1.exists()) {
                                    file1.delete();
                                    Log.d("END", "database json removed");
                                } else {
                                    Log.d("END", "database json was not exist");
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });


        // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
        startActivityForResultImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes

                            Log.d("END", "getImage");
                            try {
                                dataUri = result.getData().getData();
                                String name = dataUri.getPath();
                                int cut = name.lastIndexOf('/');
                                if (cut != -1) {
                                    name = name.substring(cut + 1);
                                }
                                Log.d("END", "Name of Image = " + name + " path = " + dataUri.getPath());
                                imagePath.setText(name);
                                imageView.setImageURI(dataUri);

                                Log.d("END", "Image = " + dataUri.toString());
                            } catch (Exception e) {
                                Log.i("END", "Some exception " + e);
                            }
                        }
                    }
                });

        //add image to database
        selectModel = findViewById(R.id.add_model);
        selectModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResultModel.launch(Intent.createChooser(intent, "Select Models"));
            }
        });

        startActivityForResultModel = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Uri data = result.getData().getData();
                            modelPath.setText(getRealPath(AddDatabase.this, data));
                            String modelToLoad = modelPath.getText().toString();
                            int index = modelToLoad.indexOf(":");
                            if (index != -1) {
                                modelToLoad = modelToLoad.substring(index+1);
                            }
                            File file = new File(modelToLoad);
                            if (file.exists()) {
                                Log.e("END", data + " - " + file.toURI() + " " + file.getAbsolutePath() + " " + String.valueOf(file.canRead()));
                            } else {
                                Log.e("END", "File is null -" + data + "-");
                            }
                        }
                    }
                });



        getSupportFragmentManager().addFragmentOnAttachListener(this);
        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragmentTemp, ArFragment.class, null)
                        .commit();
            }
        }
        findViewById(R.id.arFragmentTemp).setVisibility(View.GONE);
    }



    private void serialize(String tag, Bitmap bitmap) {
        if (database != null) {
            try {
                File file = new File(getExternalFilesDir(null) + "/dbTest.imgdb");
                databaseBuilder.addImageToDb(database, tag, bitmap);
                FileOutputStream outputStream = new FileOutputStream(file);
                database.serialize(outputStream);
                outputStream.close();
                Toast.makeText(this, "add image done: " + bitmap.toString(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Log.d("END", "database is null");
        }
    }

    public static String getJsonFromFile(File file) {
        String json = null;
        try {
            InputStream is = new FileInputStream(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static void writeJsonFile(File file, String json) {
        BufferedWriter bufferedWriter = null;
        try {

            if (!file.exists()) {
                Log.e("App","file not exist");
                file.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(json);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(AddDatabase.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragmentTemp) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
        }
    }

    public static String getRealPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}

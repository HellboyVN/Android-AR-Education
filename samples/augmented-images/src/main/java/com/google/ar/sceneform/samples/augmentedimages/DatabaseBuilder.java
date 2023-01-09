package com.google.ar.sceneform.samples.augmentedimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class DatabaseBuilder {

    private AugmentedImageDatabase database;
    // Define which image need to be detected here
    public static final String EARTH_TAG = "EARTH";
    public static final String RABBIT_TAG = "RABBIT";
    public static final String HO_CHI_MINH_TAG = "HO_CHI_MINH";
    public static final String TIGER_TAG = "TIGER";
    public static final String CAT_TAG = "CAT";
    public static final String CHICKEN_TAG = "CHICKEN";
    public static final String DOG_TAG = "DOG";
    public static final String DUCK_TAG = "DUCK";
    public static final String PIG_TAG = "PIG";
    public static final String ELEPHANT_TAG = "ELEPHANT";

    public static final String CHEMISTRY_TAG = "CHEMISTRY";
    public static final String MATH_TAG = "MATH";
    public static final String PHYSIC_TAG = "PHYSIC";
    public static final String BIO_TAG = "BIOLOGY";

    //HashMap to check if image is detected or not
    private HashMap<String, Boolean> detected;


    public DatabaseBuilder(Session session, Context c) {
        try {
            File file = new File(c.getExternalFilesDir(null) + "/dbTest.imgdb");
            if (file.exists()) {
                Log.d("END", "file exists at " + file.getAbsolutePath().toString());
                FileInputStream dbStream = new FileInputStream(file);
                database = AugmentedImageDatabase.deserialize(session, dbStream);
                Toast.makeText(c, "Database Reading done", Toast.LENGTH_SHORT).show();
            } else if (database == null) {
                Log.d("END", "file create at " + file.getAbsolutePath().toString());
                database = new AugmentedImageDatabase(session);
                addImageToDb(database,EARTH_TAG, c, R.drawable.earth);
                addImageToDb(database,HO_CHI_MINH_TAG, c, R.drawable.image);
                addImageToDb(database,TIGER_TAG, c, R.drawable.tiger);
                addImageToDb(database,CHEMISTRY_TAG, c, R.drawable.chemistry);
                addImageToDb(database,CAT_TAG, c, R.drawable.cat);
                addImageToDb(database,CHICKEN_TAG, c, R.drawable.chicken);
                addImageToDb(database,DOG_TAG, c, R.drawable.dog);
                addImageToDb(database,DUCK_TAG, c, R.drawable.duck);
                addImageToDb(database,PIG_TAG, c, R.drawable.pig);
                addImageToDb(database,ELEPHANT_TAG, c, R.drawable.elephant);
                addImageToDb(database,MATH_TAG, c, R.drawable.math);
                FileOutputStream outputStream = new FileOutputStream(file);
                database.serialize(outputStream);
                outputStream.close();
                Toast.makeText(c, "Database Initialized", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addImageToDb(AugmentedImageDatabase database, String tag, Context c, int id) {
        Bitmap bitmap = BitmapFactory.decodeResource(c.getResources(), id);
        database.addImage(tag, bitmap);
    }

    public void addImageToDb(AugmentedImageDatabase database, String tag, Bitmap bitmap) {
        database.addImage(tag, bitmap);
    }

    public AugmentedImageDatabase getDatabase() {
        return database;
    }

    public HashMap<String, Boolean> getDetectedMap() {
        return detected;
    }

    public boolean isAllImagesDetected() {
        for (String key: detected.keySet()) {
            if (!detected.get(key)) {
                return false;
            }
        }
        return true;
    }

    public void setDetectedMap(String tag, Boolean value) {
        if (detected.containsKey(tag)) {
            detected.put(tag,value);
        }
    }

}

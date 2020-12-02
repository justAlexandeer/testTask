package com.testtask.photoGallery;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.testtask.photoGallery.adapters.MainPhotoAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //RecyclerView

    //Новое окно

    //Разобраться с разрешением на загрузку и т.д. файлов

    private Button button;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String mImageFileLocation = "";
    private Uri uriPhoto;
    private String imageFileName;
    private Bitmap bitmap;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        listAll();
    }

    // Инициализация основных переменных и установка слушателя на кнопку
    private void init() {
        button = findViewById(R.id.mainActivity_ButtonMakePhoto);
        recyclerView = findViewById(R.id.mainActivity_RecyclerView);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        button.setOnClickListener(v -> {
            takePhoto();
        });
    }

    // Переходим на новую активность со взятием результата(фотографии) и сохраняем ее локально
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.provider",
                        photoFile);
                uriPhoto = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }

    // Создаем файл для картинки
    private File createImageFile() throws IOException {
        imageFileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        mImageFileLocation = image.getAbsolutePath();
        return image;
    }

    // Возрвращаем фотографию и преобразуем ее в bitmap, исходную удаляем
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == 1 && resultCode == RESULT_OK) {
            InputStream is = null;
            try {
                is = getContentResolver().openInputStream(uriPhoto);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            bitmap = BitmapFactory.decodeStream(is);
            Bitmap rotateBitmap = rotateBitmap(bitmap,mImageFileLocation);

            changerResolution(rotateBitmap);
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            getContentResolver().delete(uriPhoto, null, null);
        }
    }

    // Меняем разрешение bitmap
    private void changerResolution(Bitmap bitmap){
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                bitmap, 640, 1137, false);
        saveToByteArr(resizedBitmap);
    }

    // Сохраняем bitmap в массив байт для отправки
    private void saveToByteArr(Bitmap resizedBitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] dataImage = baos.toByteArray();
        saveImageInDataBase(dataImage);
    }

    // Отправляем фото в бд
    private void saveImageInDataBase(byte[] dataImage){
        StorageReference rf = storageReference.child("image/"+imageFileName+".jpg");

        UploadTask uploadTask = rf.putBytes(dataImage);
        uploadTask.addOnFailureListener(exception -> Toast.makeText(MainActivity.this, "Failure", Toast.LENGTH_LONG).show())
                .addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();
            listAll();
        });
    }

    //Загрузка !всех! фотографий с бд
    private void listAll() {
        StorageReference listRef = storage.getReference().child("image/");
        List<String> items = new ArrayList();
        listRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for(StorageReference item: listResult.getItems()){
                        item.getDownloadUrl().addOnCompleteListener(task -> {
                            items.add(task.getResult().toString());
                        })
                        .addOnCompleteListener(task -> {
                            updateUI(items);
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                    }
                });
    }

    //Обновление фото в UI
    private void updateUI(List<String> items){
        List<String> sortedList = new ArrayList<>();
        for(String photo : items){
            sortedList.add(photo);
        }
        Collections.sort(sortedList);
        Collections.reverse(sortedList);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(gridLayoutManager);
        MainPhotoAdapter mainPhotoAdapter = new MainPhotoAdapter(sortedList, MainActivity.this);
        recyclerView.setAdapter(mainPhotoAdapter);
    }


    // Некоторые телефоны в основном марки Samsung при возвращении фотографии из интента поворачивают
    // ее на 90 градусов. rotateBitmap() - вариант решения проблемы
    private static Bitmap rotateBitmap(Bitmap srcBitmap, String path) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(0));
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                break;
        }
        Bitmap destBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                srcBitmap.getHeight(), matrix, true);
        return destBitmap;
    }
}

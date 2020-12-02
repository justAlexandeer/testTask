package com.testtask.photoGallery;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.testtask.photoGallery.R;

public class PhotoActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        init();
    }

    private void init(){
        Intent intent = getIntent();
        String url = intent.getStringExtra("URL");
        imageView = findViewById(R.id.activityPhoto_ImageView);
        Picasso.get().load(url).into(imageView);
    }
}

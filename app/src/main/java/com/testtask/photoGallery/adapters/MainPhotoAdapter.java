package com.testtask.photoGallery.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.testtask.photoGallery.PhotoActivity;
import com.testtask.photoGallery.R;

import java.util.ArrayList;
import java.util.List;

public class MainPhotoAdapter extends RecyclerView.Adapter<MainPhotoAdapter.MyViewHolder> {

    List<String> url = new ArrayList<>();
    Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        public MyViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photoCell_image);
        }
    }

    public MainPhotoAdapter(List<String> url, Context contexte){
        this.context = contexte;
        this.url = url;
    }

    @Override
    public MainPhotoAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflate = LayoutInflater.from(parent.getContext());
        View view = inflate.inflate(R.layout.photo_cell, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MainPhotoAdapter.MyViewHolder holder, int position) {
        String stringNow = url.get(position);
        Picasso.get().load(stringNow).into(holder.imageView);
        holder.imageView.setOnClickListener(v ->{
            Intent intent = new Intent(context, PhotoActivity.class);
            intent.putExtra("URL", stringNow);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return url.size();
    }

}

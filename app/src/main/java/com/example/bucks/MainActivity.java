package com.example.bucks;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.bucks.API.ApiService;
import com.example.bucks.API.RetrofitClient;
import com.example.bucks.R;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.media.MediaPlayer;
import android.view.TextureView;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private Button generateImageButton;
    private Button downloadButton; // New button for downloading the image
    private ImageView imageView;
    private ProgressBar progressBar;
    private Handler handler;
    private Bitmap generatedBitmap; // To store the generated bitmap
    private TextureView backgroundTextureView; // Use TextureView for background
    private MediaPlayer mediaPlayer;

    private static final int STORAGE_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        generateImageButton = findViewById(R.id.generateImageButton);
        downloadButton = findViewById(R.id.downloadButton); // Initialize download button
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        backgroundTextureView = findViewById(R.id.backgroundTextureView); // Initialize TextureView

        handler = new Handler();

        // Request storage permission
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }

        // Setup video background
        setupVideoBackground();

        generateImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputText = editText.getText().toString().trim();
                if (!inputText.isEmpty()) {
                    queryApi(inputText);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter some text", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set download button click listener
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (generatedBitmap != null) {
                    saveImageToGallery(generatedBitmap);
                } else {
                    Toast.makeText(MainActivity.this, "No image to download", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ImageView leftButton = findViewById(R.id.leftButton);
        ImageView rightButton = findViewById(R.id.rightButton);

        // Navigate to Text-to-Voice Generator when left button is tapped
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TextToVoiceActivity.class);
                startActivity(intent);
            }
        });

        // Navigate to Image Generator when right button is tapped
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImageGeneratorActivity.class);
                startActivity(intent);
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private void setupVideoBackground() {
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.vid); // Your video file
        backgroundTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(getApplicationContext(), videoUri);
                    mediaPlayer.setSurface(new Surface(surface));
                    mediaPlayer.setLooping(true); // Loop the video

                    // Set the volume to 0 to mute the video
                    mediaPlayer.setVolume(0f, 0f);

                    mediaPlayer.setOnPreparedListener(mp -> {
                        mp.start(); // Start the video playback
                    });
                    mediaPlayer.prepareAsync(); // Prepare the media player
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
    }


    private void queryApi(String inputText) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Prepare the payload using the entered text
        String jsonPayload = "{\"inputs\":\"" + inputText + "\"}";
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonPayload);

        // Show progress bar and disable button
        progressBar.setVisibility(View.VISIBLE);
        generateImageButton.setEnabled(false);
        imageView.setVisibility(View.GONE); // Hide image view initially
        downloadButton.setVisibility(View.GONE); // Hide download button initially

        // Make the API call
        apiService.query(requestBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE); // Hide progress bar
                generateImageButton.setEnabled(true); // Re-enable button

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] imageBytes = response.body().bytes();
                        generatedBitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(imageBytes)); // Store the bitmap

                        // Use Glide to load the image into the ImageView with specific size
                        Glide.with(MainActivity.this)
                                .load(generatedBitmap) // Load the Bitmap
                                .override(600, 600) // Set the desired dimensions (width, height)
                                .centerCrop() // Scale the image properly
                                .into(imageView); // Set the ImageView to display the image

                        imageView.setVisibility(View.VISIBLE); // Show the image view
                        downloadButton.setVisibility(View.VISIBLE); // Show download button
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error decoding image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE); // Hide progress bar
                generateImageButton.setEnabled(true); // Re-enable button
                Toast.makeText(MainActivity.this, "Request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String savedImageURL = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Generated Image", "Image generated from app");
        if (savedImageURL != null) {
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, " ", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

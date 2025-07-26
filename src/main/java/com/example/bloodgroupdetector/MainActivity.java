package com.example.bloodgroupdetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_PERMISSION_CODE = 101;
    private static final String SERVER_URL = " http://192.168.161.86:5000/upload_fingerprint";

    private ImageView imageView;
    private ProgressBar progressBar;
    private TextView tvBloodGroup, tvError;
    private LinearLayout resultsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        tvBloodGroup = findViewById(R.id.tvBloodGroup);
        tvError = findViewById(R.id.tvError);
        resultsContainer = findViewById(R.id.resultsContainer);
        MaterialButton btnSelect = findViewById(R.id.btnSelect);

        // Check & Request Permissions
        checkPermissions();

        // Handle image selection
        btnSelect.setOnClickListener(v -> selectImageFromGallery());
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
            }
        }
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                imageView.setImageURI(imageUri);  // Show selected image
                uploadImageToServer(imageUri);   // Upload image to server
            } else {
                showError("Error: Image selection failed.");
            }
        }
    }

    private void uploadImageToServer(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        resultsContainer.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    showError("Error: Cannot open image.");
                    return;
                }

                // Convert image to byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream); // Reduce file size
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                // Create request body (multipart/form-data)
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", "fingerprint.jpg",
                                RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                        .build();

                // HTTP client
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build();

                // HTTP POST request
                Request request = new Request.Builder()
                        .url(SERVER_URL)
                        .post(requestBody)
                        .build();

                // Execute request
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d("Response", responseData);

                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            String bloodGroup = jsonResponse.optString("predicted_blood_group", "Unknown");

                            // Show result in UI
                            tvBloodGroup.setText("Blood Group: " + bloodGroup);
                            resultsContainer.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            showError("Error parsing response");
                        }
                    });
                } else {
                    showError("Server error: " + response.message());
                }
            } catch (Exception e) {
                showError("Failed to upload image. Check your connection.");
                Log.e("UploadError", "Exception: ", e);
            } finally {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        });
    }
}

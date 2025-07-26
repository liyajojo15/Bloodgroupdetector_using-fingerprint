package com.example.bloodgroupdetector;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("upload_fingerprint")  // ✅ Ensure Flask has a matching route
    Call<BloodGroupResponse> uploadFingerprint(@Part MultipartBody.Part image);
}

package com.example.bloodgroupdetector;

import com.google.gson.annotations.SerializedName;

public class BloodGroupResponse {
    @SerializedName("bloodGroup")  // Matches the JSON key "bloodGroup"
    private String bloodGroup;

    // Getter for Retrofit to access the value
    public String getBloodGroup() {
        return bloodGroup;
    }
}

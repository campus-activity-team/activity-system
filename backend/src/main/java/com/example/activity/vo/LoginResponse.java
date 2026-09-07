package com.example.activity.vo;

public record LoginResponse(String token, long expiresIn, UserProfile user) {
}

package com.example.smartpantry;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.*;
import java.io.IOException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etResetUsername, etNewPassword;
    private MaterialButton btnResetPassword;
    private TextView tvBackToLogin;

    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl = "http://192.168.1.150:5000/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etResetUsername = findViewById(R.id.etResetUsername);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnResetPassword.setOnClickListener(v -> handlePasswordReset());

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void handlePasswordReset() {
        String username = etResetUsername.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (username.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare JSON for reset (assuming backend has a /reset-password endpoint)
        String json = "{\"username\":\"" + username + "\", \"new_password\":\"" + newPassword + "\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/reset-password") // Adjust endpoint as needed
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, "Server connection failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, "Failed to update password. Check username.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}

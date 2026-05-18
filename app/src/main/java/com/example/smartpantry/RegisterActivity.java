package com.example.smartpantry;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    private EditText regUsername, regPassword;
    private Button btnRegisterSubmit;
    private TextView txtBackToLogin;
    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl = "http://10.0.2.2:5000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        regUsername = findViewById(R.id.regUsername);
        regPassword = findViewById(R.id.regPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        txtBackToLogin = findViewById(R.id.txtBackToLogin);

        btnRegisterSubmit.setOnClickListener(v -> performRegister());

        // Go back to Login page
        txtBackToLogin.setOnClickListener(v -> finish());
    }

    private void performRegister() {
        String user = regUsername.getText().toString().trim();
        String pass = regPassword.getText().toString().trim();

        if(user.isEmpty() || pass.isEmpty()) return;

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"username\":\"" + user + "\", \"password\":\"" + pass + "\"}";
        RequestBody body = RequestBody.create(json, JSON);

        // Assuming your Flask/Backend has a /register endpoint
        Request request = new Request.Builder().url(baseUrl + "/register").post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Account Created! Please Login", Toast.LENGTH_LONG).show();
                        finish(); // Returns to MainActivity
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
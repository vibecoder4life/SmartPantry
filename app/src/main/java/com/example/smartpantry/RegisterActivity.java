package com.example.smartpantry;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText regName, regEmail, regUsername, regPassword;
    private MaterialButton btnRegisterSubmit;
    private TextView txtBackToLogin;
    
    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl = "http://192.168.1.150:5000";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        regName = findViewById(R.id.regName);
        regEmail = findViewById(R.id.regEmail);
        regUsername = findViewById(R.id.regUsername);
        regPassword = findViewById(R.id.regPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        txtBackToLogin = findViewById(R.id.txtBackToLogin);

        btnRegisterSubmit.setOnClickListener(v -> performRegister());
        txtBackToLogin.setOnClickListener(v -> finish());
    }

    private void performRegister() {
        String name = regName.getText().toString().trim();
        String email = regEmail.getText().toString().trim();
        String user = regUsername.getText().toString().trim();
        String pass = regPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("full_name", name);
            jsonBody.put("email", email);
            jsonBody.put("username", user);
            jsonBody.put("password", pass);

            // Corrected parameter order for OkHttp 4.x: (MediaType, String)
            RequestBody body = RequestBody.create(JSON, jsonBody.toString());
            Request request = new Request.Builder().url(baseUrl + "/register").post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Server error. Check IP!", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(RegisterActivity.this, "Account Created! Please Login", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Registration failed (User may exist)", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
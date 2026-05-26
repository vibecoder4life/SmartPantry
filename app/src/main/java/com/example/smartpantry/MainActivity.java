package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private EditText authUsername, authPassword;
    private Button btnLogin;
    private TextView txtGoToRegister, tvForgotPassword;

    private final OkHttpClient client = new OkHttpClient();

    private final String baseUrl = "http://192.168.1.150:5000";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authUsername = findViewById(R.id.authUsername);
        authPassword = findViewById(R.id.authPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        txtGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ForgotPasswordActivity.class));
        });

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String user = authUsername.getText().toString().trim();
        String pass = authPassword.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("username", user);
            json.put("password", pass);
        } catch (Exception e) { e.printStackTrace(); }


        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder().url(baseUrl + "/login").post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Server connection failed. Check IP!", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject obj = new JSONObject(responseData);
                        int userId = obj.getInt("user_id");

                        runOnUiThread(() -> {
                            authPassword.setText("");
                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                            intent.putExtra("USER_ID", userId);
                            startActivity(intent);
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
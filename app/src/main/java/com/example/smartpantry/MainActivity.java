package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private LinearLayout authContainer, dashboardContainer;
    private EditText authUsername, authPassword, inputIngredientName, inputPrice;
    private CheckBox checkAvailable;
    private TextView txtPantryDisplay, txtGoToRegister, tvForgotPassword;
    private Button btnLogin, btnLogout, btnCreate, btnUpdate;

    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl = "http://10.0.2.2:5000";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        authContainer = findViewById(R.id.authContainer);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        authUsername = findViewById(R.id.authUsername);
        authPassword = findViewById(R.id.authPassword);
        
        // Legacy views kept for compatibility
        inputIngredientName = findViewById(R.id.inputIngredientName);
        inputPrice = findViewById(R.id.inputPrice);
        checkAvailable = findViewById(R.id.checkAvailable);
        txtPantryDisplay = findViewById(R.id.txtPantryDisplay);
        
        btnLogin = findViewById(R.id.btnLogin);
        btnLogout = findViewById(R.id.btnLogout);
        btnCreate = findViewById(R.id.btnCreate);
        btnUpdate = findViewById(R.id.btnUpdate);

        // Intent to Register Activity
        txtGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Forgot Password Logic - Navigate to ForgotPasswordActivity
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String user = authUsername.getText().toString().trim();
        String pass = authPassword.getText().toString().trim();

        if(user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String json = "{\"username\":\"" + user + "\", \"password\":\"" + pass + "\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(baseUrl + "/login").post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Server connection failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject obj = new JSONObject(responseData);
                        int userId = obj.getInt("user_id");

                        runOnUiThread(() -> {
                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                            intent.putExtra("USER_ID", userId);
                            startActivity(intent);
                            authPassword.setText("");
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}

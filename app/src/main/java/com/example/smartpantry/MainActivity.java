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
    private TextView txtPantryDisplay, txtGoToRegister;
    private Button btnLogin, btnLogout, btnCreate, btnUpdate;

    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl = "http://10.0.2.2:5000";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private int currentUserId = -1;
    private int selectedItemId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        authContainer = findViewById(R.id.authContainer);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        authUsername = findViewById(R.id.authUsername);
        authPassword = findViewById(R.id.authPassword);
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

        btnLogin.setOnClickListener(v -> performLogin());
        btnLogout.setOnClickListener(v -> handleLogout());
        btnCreate.setOnClickListener(v -> createItem());
        btnUpdate.setOnClickListener(v -> updateItem());
    }

    private void performLogin() {
        String user = authUsername.getText().toString().trim();
        String pass = authPassword.getText().toString().trim();

        if(user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare JSON for login
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
                        // 1. Get the response body
                        String responseData = response.body().string();
                        JSONObject obj = new JSONObject(responseData);

                        // 2. Extract the user_id from your backend response
                        int userId = obj.getInt("user_id");

                        runOnUiThread(() -> {
                            // 3. Create an Intent to open DashboardActivity
                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);

                            // 4. Pass the user_id to the new activity
                            intent.putExtra("USER_ID", userId);

                            // 5. Start the new page
                            startActivity(intent);

                            // Optional: Clear fields so they are empty if the user logs out later
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

    private void fetchPantryItems() {
        Request request = new Request.Builder().url(baseUrl + "/ingredients?user_id=" + currentUserId).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful() && response.body() != null) {
                    try {
                        String data = response.body().string();
                        JSONArray array = new JSONArray(data);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);
                            sb.append("ID: ").append(item.getInt("id"))
                                    .append(" | ").append(item.getString("ingredient_name"))
                                    .append("\n$").append(item.getDouble("price")).append("\n\n");
                        }
                        runOnUiThread(() -> txtPantryDisplay.setText(sb.length() == 0 ? "Empty" : sb.toString()));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void createItem() {
        String name = inputIngredientName.getText().toString().trim();
        String price = inputPrice.getText().toString().trim();
        if(name.isEmpty() || price.isEmpty()) return;

        String json = "{\"user_id\":" + currentUserId + ", \"ingredient_name\":\"" + name + "\", \"price\":" + price + ", \"is_available\":1}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(baseUrl + "/ingredients").post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) {
                if(response.isSuccessful()) {
                    runOnUiThread(() -> {
                        inputIngredientName.setText("");
                        inputPrice.setText("");
                        fetchPantryItems();
                    });
                }
            }
        });
    }

    private void updateItem() { /* ... implementation same as create but using .put() ... */ }
    private void deleteItem(int id) { /* ... implementation using .delete() ... */ }

    private void handleLogout() {
        currentUserId = -1;
        dashboardContainer.setVisibility(View.GONE);
        authContainer.setVisibility(View.VISIBLE);
    }
}
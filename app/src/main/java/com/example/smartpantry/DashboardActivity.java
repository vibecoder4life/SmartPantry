package com.example.smartpantry;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private RecyclerView rvPantry;
    private PantryAdapter adapter;
    private List<JSONObject> pantryList = new ArrayList<>();
    private int userId;
    private final OkHttpClient client = new OkHttpClient();

    private final String baseUrl = "http://192.168.1.150:5000"; 
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        userId = getIntent().getIntExtra("USER_ID", -1);

        rvPantry = findViewById(R.id.rvPantry);
        rvPantry.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(pantryList);
        rvPantry.setAdapter(adapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                fetchPantryItems();
                return true;
            } else if (id == R.id.nav_add) {
                showAddIngredientDialog();
                return true;
            } else if (id == R.id.nav_logout) {
                performLogout();
                return true;
            }
            return false;
        });

        fetchPantryItems();
    }

    private void fetchPantryItems() {

        String url = baseUrl + "/ingredients?user_id=" + userId;
        
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(DashboardActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray array = new JSONArray(responseData);
                        pantryList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            pantryList.add(array.getJSONObject(i));
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void showAddIngredientDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_ingredient, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        TextInputEditText etName = dialogView.findViewById(R.id.etIngredientName);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etIngredientPrice);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject json = new JSONObject();
                json.put("user_id", userId); // Critical: associate item with the user
                json.put("ingredient_name", name);
                json.put("price", Double.parseDouble(priceStr));
                json.put("is_available", 1);

                RequestBody body = RequestBody.create(JSON, json.toString());
                Request request = new Request.Builder().url(baseUrl + "/ingredients").post(body).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(DashboardActivity.this, "Failed to add item", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                Toast.makeText(DashboardActivity.this, "Item Added!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                fetchPantryItems(); // Refresh the list immediately
                            });
                        }
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        });

        dialog.show();
    }

    private void performLogout() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

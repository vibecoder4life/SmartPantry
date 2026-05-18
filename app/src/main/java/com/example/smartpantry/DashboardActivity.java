package com.example.smartpantry;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity implements PantryAdapter.OnItemClickListener {

    private RecyclerView rvPantry;
    private PantryAdapter adapter;
    private List<JSONObject> pantryList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    private int userId;
    private Uri selectedImageUri;
    private ImageView currentDialogImageView;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (currentDialogImageView != null) {
                        currentDialogImageView.setImageURI(selectedImageUri);
                        currentDialogImageView.setPadding(0, 0, 0, 0);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvPantry = findViewById(R.id.rvPantry);
        rvPantry.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(pantryList, this);
        rvPantry.setAdapter(adapter);

        FloatingActionButton btnAdd = findViewById(R.id.btnAddIngredient);
        btnAdd.setOnClickListener(v -> showAddEditDialog(null));

        ExtendedFloatingActionButton btnLogout = findViewById(R.id.btnBackLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LogoutActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        fetchPantryData(userId);
    }

    @Override
    public void onItemClick(JSONObject item) {
        showAddEditDialog(item);
    }

    private void showAddEditDialog(JSONObject item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_ingredient, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        currentDialogImageView = dialogView.findViewById(R.id.ivIngredientImage);
        TextInputEditText etName = dialogView.findViewById(R.id.etIngredientName);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etIngredientPrice);
        View btnSave = dialogView.findViewById(R.id.btnSave);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();

        if (item != null) {
            dialogTitle.setText("Edit Ingredient");
            try {
                etName.setText(item.getString("ingredient_name"));
                etPrice.setText(String.valueOf(item.getDouble("price")));
                // In a real app, you'd load the image URL here using Glide/Picasso
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        currentDialogImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            if (item == null) {
                addIngredient(name, price);
            } else {
                try {
                    updateIngredient(item.getInt("id"), name, price);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void fetchPantryData(int userId) {
        String url = "http://10.0.2.2:5000/ingredients?user_id=" + userId;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(DashboardActivity.this, "Error loading data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String data = response.body().string();
                        JSONArray array = new JSONArray(data);
                        pantryList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            pantryList.add(array.getJSONObject(i));
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void addIngredient(String name, double price) {
        String url = "http://10.0.2.2:5000/ingredients";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("ingredient_name", name);
            json.put("price", price);
            json.put("is_available", 1);
        } catch (Exception e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "Added successfully", Toast.LENGTH_SHORT).show();
                        fetchPantryData(userId);
                    });
                }
            }
        });
    }

    private void updateIngredient(int id, String name, double price) {
        String url = "http://10.0.2.2:5000/ingredients/" + id;
        JSONObject json = new JSONObject();
        try {
            json.put("ingredient_name", name);
            json.put("price", price);
            json.put("is_available", 1);
        } catch (Exception e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).put(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                        fetchPantryData(userId);
                    });
                }
            }
        });
    }
}

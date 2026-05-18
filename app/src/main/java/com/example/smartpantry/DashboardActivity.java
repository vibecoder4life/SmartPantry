package com.example.smartpantry;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    private final OkHttpClient client = new OkHttpClient();
    private int userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Retrieve the ID passed from MainActivity
        int userId = getIntent().getIntExtra("USER_ID", -1);

        if (userId == -1) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            finish(); // Go back if no ID is found
            return;
        }

        // Now call your fetch function using this ID
        fetchPantryData(userId);
    }

    // Add "int userId" inside the parentheses
    private void fetchPantryData(int userId) {
        // Use the userId passed into the method to build the URL
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
}

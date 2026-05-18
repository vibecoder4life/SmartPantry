package com.example.smartpantry;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.util.List;

public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {
    private List<JSONObject> list;

    public PantryAdapter(List<JSONObject> list) { this.list = list; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject item = list.get(position);
            holder.name.setText(item.getString("ingredient_name"));
            holder.price.setText("$" + item.getDouble("price"));
            int available = item.getInt("is_available");
            holder.status.setText(available == 1 ? "In Stock" : "Out of Stock");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, price, status;
        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvIngredientName);
            price = itemView.findViewById(R.id.tvPrice);
            status = itemView.findViewById(R.id.tvStatus);
        }
    }
}
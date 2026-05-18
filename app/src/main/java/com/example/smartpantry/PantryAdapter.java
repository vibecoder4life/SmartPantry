package com.example.smartpantry;

import android.graphics.Color;
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
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(JSONObject item);
    }

    public PantryAdapter(List<JSONObject> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

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
            holder.price.setText("$" + String.format("%.2f", item.getDouble("price")));
            
            int available = item.getInt("is_available");
            if (available == 1) {
                holder.status.setText("In Stock");
                holder.status.setTextColor(Color.parseColor("#388E3C")); // Green
            } else {
                holder.status.setText("Out of Stock");
                holder.status.setTextColor(Color.parseColor("#E53E3E")); // Red
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
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
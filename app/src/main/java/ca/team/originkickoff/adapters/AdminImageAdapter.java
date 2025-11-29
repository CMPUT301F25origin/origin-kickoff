package ca.team.originkickoff.adapters;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ca.team.originkickoff.R;

/**
 * Grid adapter for admin image browser, showing thumbnails and a delete action.
 */
public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.Holder> {
    public static class Item {
        public final String id;
        public final String title; // retained but not displayed
        public final String url;
        public final String storagePath;
        public final String kind; // "event" or "user"
        public final byte[] bytes; // optional raw image bytes
        public Item(String id, String title, String url, String storagePath, String kind) {
            this(id, title, url, storagePath, kind, null);
        }
        public Item(String id, String title, String url, String storagePath, String kind, byte[] bytes) {
            this.id = id; this.title = title; this.url = url; this.storagePath = storagePath; this.kind = kind; this.bytes = bytes;
        }
    }

    public interface Listener {
        void onDelete(Item item);
    }

    private final List<Item> items = new ArrayList<>();
    private final Listener listener;

    public AdminImageAdapter(Listener l) {
        this.listener = l;
    }

    public void setItems(List<Item> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_image, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Item it = items.get(position);
        if (it.bytes != null && it.bytes.length > 0) {
            h.ivThumb.setImageBitmap(BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.length));
        } else {
            Glide.with(h.itemView.getContext())
                    .load(it.url)
                    .placeholder(R.drawable.sample_event_1)
                    .error(R.drawable.sample_event_1)
                    .into(h.ivThumb);
        }
        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(it); });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView ivThumb; ImageView btnDelete;
        Holder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

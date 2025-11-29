package ca.team.originkickoff.adapters;

import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

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

    public void setItems(List<Item> list) {
        items.clear();
        if (list != null) {
            for (Item it : list) {
                boolean hasUrl = it != null && it.url != null && !it.url.isEmpty();
                boolean hasBytes = it != null && it.bytes != null && it.bytes.length > 0;
                if (hasUrl || hasBytes) {
                    items.add(it);
                }
            }
        }
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
            android.graphics.Bitmap bmp = BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.length);
            if (bmp != null) {
                h.ivThumb.setImageBitmap(bmp);
            } else {
                // Failed to decode bytes; remove tile using current adapter position
                int adapterPos = h.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) removeAt(adapterPos);
                return;
            }
        } else if (!TextUtils.isEmpty(it.url)) {
            Glide.with(h.itemView.getContext())
                    .load(it.url)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            int adapterPos = h.getBindingAdapterPosition();
                            if (adapterPos != RecyclerView.NO_POSITION) {
                                removeAt(adapterPos);
                            }
                            return true; // consume failure
                        }
                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(h.ivThumb);
        } else {
            int adapterPos = h.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) removeAt(adapterPos);
            return;
        }
        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(it); });
    }

    private void removeAt(int position) {
        if (position < 0 || position >= items.size()) return;
        items.remove(position);
        notifyItemRemoved(position);
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

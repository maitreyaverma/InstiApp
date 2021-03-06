package app.insti.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import app.insti.ItemClickListener;
import app.insti.R;
import app.insti.data.Body;


public class BodyAdapter extends RecyclerView.Adapter<BodyAdapter.ViewHolder> {

    private List<Body> bodyList;
    private ItemClickListener itemClickListener;
    private Context context;

    public BodyAdapter(List<Body> bodyList, ItemClickListener itemClickListener) {
        this.bodyList = bodyList;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context)
                .inflate(R.layout.body_card_view, parent, false);
        final ViewHolder postViewHolder = new ViewHolder(v);

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemClickListener.onItemClick(view, postViewHolder.getAdapterPosition());
            }
        });

        return postViewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Body body = bodyList.get(position);
        holder.name.setText(body.getBodyName());
        holder.description.setText(body.getBodyShortDescription());
        Picasso.get().load(body.getBodyImageURL()).into(holder.image);

    }

    @Override
    public int getItemCount() {
        return bodyList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public TextView description;
        public ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.body_card_name);
            description = (TextView) itemView.findViewById(R.id.body_card_description);
            image = (ImageView) itemView.findViewById(R.id.body_card_avatar);
        }


    }
}

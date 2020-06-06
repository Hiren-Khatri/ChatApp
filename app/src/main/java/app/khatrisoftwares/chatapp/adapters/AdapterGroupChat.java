package app.khatrisoftwares.chatapp.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.models.ModelGroupChat;

public class AdapterGroupChat extends RecyclerView.Adapter<AdapterGroupChat.HolderGroupChat> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private ArrayList<ModelGroupChat> modelGroupChatList;
    private FirebaseAuth firebaseAuth;

    public AdapterGroupChat(Context context, ArrayList<ModelGroupChat> modelGroupChatList) {
        this.context = context;
        this.modelGroupChatList = modelGroupChatList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderGroupChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_right,parent,false);
            return new HolderGroupChat(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_left,parent,false);
            return new HolderGroupChat(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull HolderGroupChat holder, int position) {
        ModelGroupChat groupChat = modelGroupChatList.get(position);
        String senderUid = groupChat.getSender();
        String message = groupChat.getMessage();
        String timestamp = groupChat.getTimestamp();
        String messageType = groupChat.getType();

        //convert timestamp to dd/MM/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        if (messageType.equals("text")){
            //text message : hide image view and show text view
            holder.messageIV.setVisibility(View.GONE);
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageTv.setText(message);

        } else {
            //image message : hide text view and show image view
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIV.setVisibility(View.VISIBLE);
            try {
                Picasso.get().load(message).placeholder(R.drawable.ic_image_gray).into(holder.messageIV);
            } catch (Exception e){
                holder.messageIV.setImageResource(R.drawable.ic_image_gray);
            }
        }

        //set data
        holder.timeTv.setText(dateTime);

        setUsername(groupChat,holder);
    }

    private void setUsername(ModelGroupChat groupChat, final HolderGroupChat holder) {
        //get sender info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(groupChat.getSender())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            String name = ""+ds.child("name").getValue();

                            holder.nameTv.setText(name);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public int getItemViewType(int position) {
        if (modelGroupChatList.get(position).getSender().equals(firebaseAuth.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    @Override
    public int getItemCount() {
        return modelGroupChatList.size();
    }

    class HolderGroupChat extends RecyclerView.ViewHolder{

        private TextView nameTv,messageTv,timeTv;
        private ImageView messageIV;

        public HolderGroupChat(@NonNull View itemView) {
            super(itemView);

            nameTv = itemView.findViewById(R.id.nameTv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            messageIV = itemView.findViewById(R.id.messageIV);
        }
    }
}

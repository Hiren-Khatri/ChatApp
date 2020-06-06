package app.khatrisoftwares.chatapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.models.ModelChat;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.HolderChat> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    private Context context;
    List<ModelChat> chatList;
    String imageUri;

    private FirebaseUser fUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUri) {
        this.context = context;
        this.chatList = chatList;
        this.imageUri = imageUri;
    }

    @NonNull
    @Override
    public HolderChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layouts:row_chat_left:for receiver,row_chat_right:for sender
        if (viewType == MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right,parent,false);
            return new HolderChat(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left,parent,false);
            return new HolderChat(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull HolderChat holder, final int position) {
        String message = chatList.get(position).getMessage();
        String timestamp = chatList.get(position).getTimestamp();
        String type = chatList.get(position).getType();

        //convert timestamp to dd/MM/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        if (type.equals("text")){
            //text message
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);

            holder.messageTv.setText(message);
        } else {
            //image message
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);

            Picasso.get().load(message).placeholder(R.drawable.ic_image_gray).into(holder.messageIv);
        }

        //set data
        holder.timeTv.setText(dateTime);
//        holder.messageTv.setText(message);
        try {
            Picasso.get().load(imageUri).into(holder.profileIv);
        } catch (Exception e){

        }
        //click to show delete dialog

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //show delete message confirm dialog
                AlertDialog.Builder builder  = new AlertDialog.Builder(context);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure you want to delete this message?");
                //delete button
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMessage(position);
                    }
                })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                //create and show dialog
                builder.create().show();
                return false;
            }
        });

//        holder.messageLl.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
        if (position==chatList.size()-1){
            if (chatList.get(position).isSeen()){
                holder.isSeenTv.setText("Seen");
            } else {
                holder.isSeenTv.setText("Delivered");
            }
        } else {
            holder.isSeenTv.setVisibility(View.GONE);
        }
    }

    private void deleteMessage(int position) {
        final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //get timestamp of clicked image
//        deleted msg where timestamp matches delete that msg
        String timestamp = chatList.get(position).getTimestamp();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = ref.orderByChild("timestamp").equalTo(timestamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){

                    if (ds.child("sender").getValue().equals(myUid)){
//                        ds.getRef().removeValue();
                        HashMap<String ,Object> hashMap = new HashMap<>();
                        hashMap.put("message","This message was deleted...");
                        ds.getRef().updateChildren(hashMap);
                        Toast.makeText(context, "message deleted...", Toast.LENGTH_SHORT).show();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(fUser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    class HolderChat extends RecyclerView.ViewHolder{

        private ImageView profileIv,messageIv;
        private TextView messageTv,timeTv,isSeenTv;
        private LinearLayout messageLl;
        public HolderChat(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            messageIv = itemView.findViewById(R.id.messageIV);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            isSeenTv = itemView.findViewById(R.id.isSeenTv);
            messageLl = itemView.findViewById(R.id.messageLl);
        }
    }
}

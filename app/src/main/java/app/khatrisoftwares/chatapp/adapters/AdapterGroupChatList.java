package app.khatrisoftwares.chatapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import app.khatrisoftwares.chatapp.GroupChatActivity;
import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.models.ModelGroupChatList;

public class AdapterGroupChatList extends RecyclerView.Adapter<AdapterGroupChatList.HolderGroupChats> {

    private Context context;
    private ArrayList<ModelGroupChatList> groupChatsList;

    public AdapterGroupChatList(Context context, ArrayList<ModelGroupChatList> groupChatsList) {
        this.context = context;
        this.groupChatsList = groupChatsList;
    }

    @NonNull
    @Override
    public HolderGroupChats onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_list,parent,false);
        return new HolderGroupChats(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderGroupChats holder, int position) {
        //get data
        ModelGroupChatList model = groupChatsList.get(position);
        final String groupId = model.getGroupId();
        String groupTitle = model.getGroupTitle();
        String groupIcon = model.getGroupIcon();

        holder.nameTv.setText("");
        holder.timeTv.setText("");
        holder.messageTv.setText("");

        //load last message and message-time
        loadLastMessage(model,holder);

        holder.groupTitleTv.setText(groupTitle);
        try{
            Picasso.get().load(groupIcon).placeholder(R.drawable.ic_group_primary).into(holder.groupIconIv);
        } catch (Exception e){
            holder.groupIconIv.setImageResource(R.drawable.ic_group_primary);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start groupChatActivity
                Intent intent = new Intent(context, GroupChatActivity.class);
                intent.putExtra("groupId",groupId);
                context.startActivity(intent);
            }
        });

    }

    private void loadLastMessage(ModelGroupChatList model, final HolderGroupChats holder) {
        //get last message from group
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(model.getGroupId()).child("Messages").limitToLast(1) //get last item(message) frm that child
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){

                            //get data
                            String message = ""+ds.child("message").getValue();
                            String timestamp = ""+ds.child("timestamp").getValue();
                            String sender = ""+ds.child("sender").getValue();
                            String messageType = ""+ds.child("type").getValue();

                            //convert time
                            //convert timestamp to dd/MM/yyyy hh:mm am/pm
                            Calendar calendar = Calendar.getInstance(Locale.getDefault());
                            calendar.setTimeInMillis(Long.parseLong(timestamp));
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

                            if (messageType.equals("image")){
                                holder.messageTv.setText("Sent a photo");
                            } else {
                                holder.messageTv.setText(message);
                            }
                            holder.timeTv.setText(dateTime);

                            //get info of sender of last message
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                            ref.orderByChild("uid").equalTo(sender)
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
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return groupChatsList.size();
    }


    class HolderGroupChats extends RecyclerView.ViewHolder{

        private ImageView groupIconIv;
        private TextView groupTitleTv,nameTv,messageTv,timeTv;

        public HolderGroupChats(@NonNull View itemView) {
            super(itemView);
            groupIconIv = itemView.findViewById(R.id.groupIconIv);
            groupTitleTv = itemView.findViewById(R.id.groupTitleTv);
            nameTv = itemView.findViewById(R.id.nameTv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}

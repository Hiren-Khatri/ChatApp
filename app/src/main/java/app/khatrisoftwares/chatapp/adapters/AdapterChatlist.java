package app.khatrisoftwares.chatapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import app.khatrisoftwares.chatapp.activities.ChatActivity;
import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.models.ModelUser;

public class AdapterChatlist extends RecyclerView.Adapter<AdapterChatlist.HolderChatlist> {
    Context context;
    List<ModelUser> userList;
    private HashMap<String,String> lastMessageMap;

    public AdapterChatlist(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        lastMessageMap = new HashMap<>();
    }

    @NonNull
    @Override
    public HolderChatlist onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //item layout row_chatlist.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_chatlist,parent,false);
        return new HolderChatlist(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderChatlist holder, int position) {
        //get data
        final String hisUid = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        String lastMessage = lastMessageMap.get(hisUid);

        //set data
        holder.nameTv.setText(userName);
        if (lastMessage == null || lastMessage.equals("default")){
            holder.lastMessageTv.setVisibility(View.GONE);
        } else {
            holder.lastMessageTv.setVisibility(View.VISIBLE);
            holder.lastMessageTv.setText(lastMessage);
        }
        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_face_primary).into(holder.profileIv);
        } catch (Exception e){

        }

        //set online status of other users in chatlist
        if (userList.get(position).getOnlineStatus().equals("online")){
            //online
            holder.onlineStatusIv.setImageResource(R.drawable.circle_online);
        } else {
            holder.onlineStatusIv.setImageResource(R.drawable.circle_offline);
        }
        //handle click listener of user in chatlist
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start chat activity with the user
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("hisUid",hisUid);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setLastMessageMap(String userId,String lastMessage){
        lastMessageMap.put(userId,lastMessage);
    }

    class HolderChatlist extends RecyclerView.ViewHolder{

        //views of row_chatlist.xml
        ImageView profileIv,onlineStatusIv;
        TextView nameTv,lastMessageTv;

        public HolderChatlist(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            lastMessageTv = itemView.findViewById(R.id.lastMessageTv);
        }
    }
}

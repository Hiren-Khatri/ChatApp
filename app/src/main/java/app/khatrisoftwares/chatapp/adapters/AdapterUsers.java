package app.khatrisoftwares.chatapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import app.khatrisoftwares.chatapp.ChatActivity;
import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.ThereProfileActivity;
import app.khatrisoftwares.chatapp.models.ModelUser;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.HolderUsers> {
    private Context context;
    private List<ModelUser> userList;
    FirebaseAuth firebaseAuth;
    String myUid;

    public AdapterUsers(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;

        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public HolderUsers onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_users,parent,false);
        return new HolderUsers(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderUsers holder, final int position) {
        final ModelUser modelUser = userList.get(position);
        holder.nameTv.setText(modelUser.getName());
        holder.emailTv.setText(modelUser.getEmail());
        try {
            Picasso.get().load(modelUser.getImage()).placeholder(R.drawable.ic_face_primary).into(holder.profileIv);
        }catch (Exception e){
            holder.profileIv.setImageResource(R.drawable.ic_face_primary);
        }

        holder.blockIv.setImageResource(R.drawable.ic_unblocked_green);
        checkIsBlocked(modelUser.getUid(),holder,position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0){
                            //profile clicked
                            Intent intent = new Intent(context, ThereProfileActivity.class);
                            intent.putExtra("uid",modelUser.getUid());
                            context.startActivity(intent);
                        } if (which ==1){
                            //Chat clicked
                            imBlockedORNot(modelUser.getUid());
                        }

                    }
                });
                builder.create().show();
            }
        });

        holder.blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userList.get(position).isBlocked()){
                    unBlockUser(modelUser.getUid());
                } else {
                    blockUser(modelUser.getUid());
                }
            }
        });
    }

    private void imBlockedORNot(final String hisUid){
        //first check if sender is blocked by receiver or not
        //Logic: if uid of the sender(currentUser) exists in BlockedUsers of receiver then sender(currentUser) is blocked ,otherwise not
        //if blocked then just display a message e.g. You're blocked by user ,can't send message
        //if not blocked then simply start ChatActivity

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("BlockedUsers").orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage("You are blocked by this user, can't send message...");
                                builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                builder.create().show();
                                return;
                            }
                        }
                        //not blocked ,start ChatActivity
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("hisUid",hisUid);
                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void checkIsBlocked(String uid, final HolderUsers holder, final int position) {
        //check each user if blocked or not
//        if uid of that user exists in "BlockedUsers" then that user is blocked ,other wise not
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                holder.blockIv.setImageResource(R.drawable.ic_blocked_red);
                                userList.get(position).setBlocked(true);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void blockUser(String uid) {
        //put value in hashMap
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid",uid);

        //block user by adding his uid to current user's "BlockedUsers" node
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(uid).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //blocked user
                        Toast.makeText(context, "Blocked User", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockUser(String uid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(context, "Unblocked User", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(context, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
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
        return userList.size();
    }

    //holder class
    class HolderUsers extends RecyclerView.ViewHolder{

        private ImageView profileIv,blockIv;
        private TextView nameTv,emailTv;

        public HolderUsers(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            blockIv = itemView.findViewById(R.id.blockIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            emailTv = itemView.findViewById(R.id.emailTv);
        }
    }
}

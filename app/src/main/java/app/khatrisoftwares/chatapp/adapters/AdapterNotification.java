package app.khatrisoftwares.chatapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateFormat;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import app.khatrisoftwares.chatapp.activities.PostDetailActivity;
import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.models.ModelNotification;

public class AdapterNotification  extends RecyclerView.Adapter<AdapterNotification.HolderNotification> {

    private Context context;
    private ArrayList<ModelNotification> notificationsList;
    private FirebaseAuth firebaseAuth;

    public AdapterNotification(Context context, ArrayList<ModelNotification> notificationsList) {
        this.context = context;
        this.notificationsList = notificationsList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderNotification onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_notification,parent,false);
        return new HolderNotification(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final HolderNotification holder, int position) {
        //get and set data
        final ModelNotification model = notificationsList.get(position);
//        String name = model.getsName();
        String notification = model.getNotification();
//        String image = model.getsImage();
        final String timestamp = model.getTimestamp();
        String senderUid = model.getsUid();
        final String pId = model.getpId();

        //convert timestamp into dd/mm/yyyy hh:mm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String pTimeFormat = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        //get hisname,dp,email from database
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(senderUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            String name = ""+ds.child("name").getValue();
                            String image = ""+ds.child("image").getValue();
                            String email = ""+ds.child("email").getValue();

                            model.setsName(name);
                            model.setsImage(image);
                            model.setsEmail(email);

                            holder.nameTv.setText(name);
                            try{
                                Picasso.get().load(image).placeholder(R.drawable.ic_face_primary).into(holder.avatarIv);
                            } catch (Exception e){
                                holder.avatarIv.setImageResource(R.drawable.ic_face_primary);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


        //set to views
        holder.notificationTv.setText(notification);
        holder.timeTv.setText(pTimeFormat);

        //click notification to open post
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId",pId);
                context.startActivity(intent);
            }
        });

        //long press to delete notification
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //show confirm delete dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete this notification");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //delete notification
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                        ref.child(firebaseAuth.getUid()).child("Notifications").child(timestamp)
                                .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(context, "Notification Deleted", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                  @Override
                    public void onClick(DialogInterface dialog, int which) {
                      dialog.dismiss();
                     }
                });
                builder.create().show();
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    class HolderNotification extends RecyclerView.ViewHolder{

        private ImageView avatarIv;
        private TextView nameTv,notificationTv,timeTv;

        public HolderNotification(@NonNull View itemView) {
            super(itemView);
            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            notificationTv = itemView.findViewById(R.id.notificationTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}

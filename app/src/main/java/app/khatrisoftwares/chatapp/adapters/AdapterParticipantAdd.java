package app.khatrisoftwares.chatapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.models.ModelUser;

public class AdapterParticipantAdd extends RecyclerView.Adapter<AdapterParticipantAdd.HolderParticipantAdd> {
    private Context context;
    private ArrayList<ModelUser> userList;
    private String groupId, myGroupRole;//role:creator/admin/participant

    public AdapterParticipantAdd(Context context, ArrayList<ModelUser> userList, String groupId, String myGroupRole) {
        this.context = context;
        this.userList = userList;
        this.groupId = groupId;
        this.myGroupRole = myGroupRole;
    }

    @NonNull
    @Override
    public HolderParticipantAdd onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_participant_add, parent, false);
        return new HolderParticipantAdd(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderParticipantAdd holder, int position) {

        final ModelUser user = userList.get(position);
        final String name = user.getName();
        String email = user.getEmail();
        String image = user.getImage();
        final String uid = user.getUid();

        //set view
        holder.nameTv.setText(name);
        holder.emailTv.setText(email);
        try {
            Picasso.get().load(image).placeholder(R.drawable.ic_face_primary).into(holder.profileIv);
        } catch (Exception e) {
            holder.profileIv.setImageResource(R.drawable.ic_face_primary);
        }

        checkIfAlreadyExists(user, holder);

        //handle click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                /* Checkif user already added or not
                * if added : show remove-participant/make-admin/remove-admin option
                * if not added ,show add participant option*/
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
                ref.child(groupId).child("Participants").child(uid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()){
                                    //user exists/participant
                                    String hisPrevRole = ""+dataSnapshot.child("role").getValue();
                                    //option to display in dialog
                                    String[] options;

                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Choose Option");
                                    if (myGroupRole.equals("creator")){
                                        if (hisPrevRole.equals("admin")){
                                            //case: im creator,he is admin
                                            options = new String[]{"Remove Admin","Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //item clicks
                                                    if (which == 0){
                                                        //remove admin clciked
                                                        removeAdmin(user);
                                                    } else  if (which == 1){
                                                        //remove user clicked
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                        else if (hisPrevRole.equals("participant")){
                                            // im creator ,he is participant
                                            options = new String[]{"Make Admin","Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    if (which == 0){
                                                        //make admin clicked
                                                        makeAdmin(user);
                                                    } else if (which == 1){
                                                        //remove user clicked
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                    }
                                    else if (myGroupRole.equals("admin")){
                                        if (hisPrevRole.equals("creator")){
                                            //case: im admin he is creator
                                            Snackbar.make(v,"Creator of Group",Snackbar.LENGTH_SHORT).setBackgroundTint(context.getColor(R.color.colorPrimaryDark)).show();
                                        }
                                        else if (hisPrevRole.equals("admin")){
                                            //case: im admin,he is admin
                                            options = new String[]{"Remove Admin","Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //item clicks
                                                    if (which == 0){
                                                        //remove admin clciked
                                                        removeAdmin(user);
                                                    } else  if (which == 1){
                                                        //remove user clicked
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                        else if (hisPrevRole.equals("participant")){
                                            //im admin he is participant
                                            options = new String[]{"Make Admin","Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    if (which == 0){
                                                        //make admin clicked
                                                        makeAdmin(user);
                                                    } else if (which == 1){
                                                        //remove user clicked
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                    }
                                } else {
                                    //user doesn't exists /show add participant to admin/creator
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Add Participant")
                                            .setMessage("Add this user in this group?")
                                            .setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //add user
                                                    addParticipant(user);
                                                }
                                            })
                                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            }).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });
    }

    private void addParticipant(ModelUser user) {
        //set up data
        String timestamp = String.valueOf(System.currentTimeMillis());
        HashMap<String ,String> hashMap = new HashMap<>();
        hashMap.put("uid",user.getUid());
        hashMap.put("role","participant");
        hashMap.put("timestamp",timestamp);

        //add user in this group
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").child(user.getUid()).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //added successfully
                        Toast.makeText(context, "Added Successfully...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void makeAdmin(final ModelUser user) {
        //setup data
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("role","admin");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").child(user.getUid()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //made admin
                        Toast.makeText(context, user.getName()+" is now admin!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeParticipant(final ModelUser user) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").child(user.getUid())
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
//                        Toast.makeText(context, user.getName()+" removed from this group", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeAdmin(final ModelUser user) {
        //change role to participant
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("role","participant");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").child(user.getUid()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //made admin
                        Toast.makeText(context, user.getName()+" is no longer admin...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfAlreadyExists(ModelUser user, final HolderParticipantAdd holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            //already exists
                            String hisRole = ""+dataSnapshot.child("role").getValue();
                            holder.statusTv.setText(hisRole);
                        } else {
                            holder.statusTv.setText("");
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

    class HolderParticipantAdd extends RecyclerView.ViewHolder {
        private ImageView profileIv;
        private TextView nameTv, emailTv, statusTv;

        public HolderParticipantAdd(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            emailTv = itemView.findViewById(R.id.emailTv);
            statusTv = itemView.findViewById(R.id.statusTv);
        }
    }
}

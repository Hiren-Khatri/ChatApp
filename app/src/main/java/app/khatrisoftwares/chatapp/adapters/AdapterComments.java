package app.khatrisoftwares.chatapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.models.ModelComment;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.HolderComment> {
    Context context;
    List<ModelComment> commentList;
    String myUid,postId;

    public AdapterComments(Context context, List<ModelComment> commentList, String myUid, String postId) {
        this.context = context;
        this.commentList = commentList;
        this.myUid = myUid;
        this.postId = postId;
    }

    @NonNull
    @Override
    public HolderComment onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_comments, parent, false);
        return new HolderComment(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderComment holder, int position) {
        final String uid = commentList.get(position).getUid();
        String uName = commentList.get(position).getuName();
        String uEmail = commentList.get(position).getuEmail();
        String uDp = commentList.get(position).getuDp();
        final String cId = commentList.get(position).getcId();
        String timestamp = commentList.get(position).getTimestamp();
        String comment = commentList.get(position).getComment();

        //convert timestamp into dd/mm/yyyy hh:mm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String pTimeFormat = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        holder.nameTv.setText(uName);
        holder.commentTv.setText(comment);
        holder.timeTv.setText(pTimeFormat);
        try{
            Picasso.get().load(uDp).placeholder(R.drawable.ic_face_primary).into(holder.avatarIv);
        } catch (Exception e){
        }

        //set on long click listener
       holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
           @Override
           public boolean onLongClick(View v) {
               if (myUid.equals(uid)){
                   //my comment
                   AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());
                   builder.setTitle("Delete");
                   builder.setMessage("Are you sure you want to delete this comment permanently?");
                   builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                            //delete comment
                           deleteComment(cId);
                       }
                   });
                   builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                       }
                   });
                   builder.create().show();

               } else {
//                   not my comment
               }
               return true;
           }
       });
    }

    private void deleteComment(String cId) {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
              ref.child("Comments").child(cId).removeValue();

              ref.addListenerForSingleValueEvent(new ValueEventListener() {
                  @Override
                  public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                      String comments = "" + dataSnapshot.child("pComments").getValue();
                      int newCommentVal = Integer.parseInt(comments) - 1;
                      ref.child("pComments").setValue("" + newCommentVal);
                  }

                  @Override
                  public void onCancelled(@NonNull DatabaseError databaseError) {

                  }
              });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }


    class HolderComment extends RecyclerView.ViewHolder {

        //views
        ImageView avatarIv;
        TextView nameTv, commentTv, timeTv;

        public HolderComment(@NonNull View itemView) {
            super(itemView);
            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            commentTv = itemView.findViewById(R.id.commentTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}

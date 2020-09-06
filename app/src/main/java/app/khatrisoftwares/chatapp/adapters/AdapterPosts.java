package app.khatrisoftwares.chatapp.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import app.khatrisoftwares.chatapp.activities.AddPostActivity;
import app.khatrisoftwares.chatapp.activities.PostDetailActivity;
import app.khatrisoftwares.chatapp.activities.PostLikedActivity;
import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.activities.ThereProfileActivity;
import app.khatrisoftwares.chatapp.models.ModelPost;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.HolderPosts> {

    Context context;
    List<ModelPost> postList;
    String myUid;

    private DatabaseReference likesRef; //for likes database node
    private DatabaseReference postsRef;//for posts database node

    boolean mProcessLike = false,isOnProfilePage;

    public AdapterPosts(Context context, List<ModelPost> postList,boolean isOnProfilePage) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference("Posts");
         this.isOnProfilePage=isOnProfilePage;
    }

    @NonNull
    @Override
    public HolderPosts onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, parent, false);
        return new HolderPosts(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final HolderPosts holder, final int position) {
        final String uid = postList.get(position).getUid();
        String uEmail = postList.get(position).getuEmail();
        String uName = postList.get(position).getuName();
        String uDp = postList.get(position).getuDp();
        final String pId = postList.get(position).getpId();
        final String pTitle = postList.get(position).getpTitle();
        final String pDesc = postList.get(position).getpDesc();
        final String pImage = postList.get(position).getpImage();
        String pTime = postList.get(position).getpTime();
        String pLikes = postList.get(position).getpLikes(); //contains total number of likes of that post
        String pComments = postList.get(position).getpComments(); //contains total number of likes of that post

        //convert timestamp into dd/mm/yyyy hh:mm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTime));
        String pTimeFormate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();


        holder.uNameTv.setText(uName);
        holder.pTimeTv.setText(pTimeFormate);
        holder.pTitleTv.setText(pTitle);
        holder.pDescriptionTv.setText(pDesc);
        holder.pLikesTv.setText(pLikes + " Likes"); //e.g. 100 Likes
        holder.pCommentsTv.setText(pComments + " Comments"); //e.g. 100 Comments
        setLikes(holder, pId);//set likes for each post

        try {
            Picasso.get().load(uDp).placeholder(R.drawable.ic_face_primary).into(holder.uPictureIv);
        } catch (Exception e) {
        }

        //if there is no image hide post image
        if (pImage.equals("noImage")) {
            holder.pImageIv.setVisibility(View.GONE);
        } else {
            holder.pImageIv.setVisibility(View.VISIBLE);
            try {
                Picasso.get().load(pImage).into(holder.pImageIv);
            } catch (Exception e) {
            }
        }

        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(context, "More", Toast.LENGTH_SHORT).show();
                showMoreOptions(holder.moreBtn, uid, myUid, pId, pImage);
            }
        });
        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(context, "Like", Toast.LENGTH_SHORT).show();
                final int pLikes = Integer.parseInt(postList.get(position).getpLikes());
                mProcessLike = true;
                //get id of the post clicked
                final String postId = postList.get(position).getpId();

                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (mProcessLike) {
                            if (dataSnapshot.child(postId).hasChild(myUid)) {
                                //already liked so remove like
                                postsRef.child(postId).child("pLikes").setValue("" + (pLikes - 1));
                                likesRef.child(postId).child(myUid).removeValue();
                                mProcessLike = false;
                            } else {
                                //not liked ,like post
                                postsRef.child(postId).child("pLikes").setValue("" + (pLikes + 1));
                                likesRef.child(postId).child(myUid).setValue("Liked");
                                mProcessLike = false;

                                addToHisNotifications("" + uid, "" + pId, "Likes your post");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
        holder.commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(context, "Comment", Toast.LENGTH_SHORT).show();
                //start  PostDetailActivity
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", pId);
                context.startActivity(intent);
            }
        });
        holder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(context, "Share", Toast.LENGTH_SHORT).show();
                /*Some posts contains only text ,and some contains both text and images ,here both cases are handled*/
                //get image from image view
                BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.pImageIv.getDrawable();

                if (bitmapDrawable == null) {
                    //post without image
                    shareTextOnly(pTitle, pDesc);
                } else {
                    //post with image
                    //convert image into bitmap
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle, pDesc, bitmap);
                }
            }
        });
        holder.profileLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go to ThereProfileActivity with uid of that user
                if(!isOnProfilePage) {
                    Intent intent = new Intent(context, ThereProfileActivity.class);
                    intent.putExtra("uid", uid);
                    context.startActivity(intent);
                }
            }
        });

        holder.pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PostLikedActivity.class);
                intent.putExtra("postId",pId);
                context.startActivity(intent);
            }
        });
    }

    private void addToHisNotifications(String hisUid, String pId, String notification) {
        String timestamp = "" + System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);

        if (!hisUid.equals(myUid)) {

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //added successfully
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed
                        }
                    });
        }
    }

    private void shareTextOnly(String pTitle, String pDesc) {
        //concatenate title and description to share
        String sharedBody = pTitle + "\n" + pDesc;

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");  // in case of sharing via email
        sIntent.putExtra(Intent.EXTRA_TEXT, sharedBody); //text to share
        context.startActivity(Intent.createChooser(sIntent, "Share Via")); //msg to show in share dialog
    }

    private void shareImageAndText(String pTitle, String pDesc, Bitmap bitmap) {
        //concatenate title and description to share
        String sharedBody = pTitle + "\n" + pDesc;

        //first save image in cache, get saved image uri
        Uri uri = saveImageToShare(bitmap);

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");  // in case of sharing via email
        sIntent.putExtra(Intent.EXTRA_TEXT, sharedBody); //text to share
        sIntent.setType("image/png");
        context.startActivity(Intent.createChooser(sIntent, "Share Via")); //msg to show in share dialog
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;
        try {
            imageFolder.mkdirs(); //create if not exists
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, "app.khatrisoftwares.chatapp.fileprovider", file);
        } catch (Exception e) {
            Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void setLikes(final HolderPosts holder, final String postId) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postId).hasChild(myUid)) {
                    //user has liked the post
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked_primary, 0, 0, 0);
                    holder.likeBtn.setText("Liked");
                } else {
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                    holder.likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showMoreOptions(ImageButton moreBtn, String uid, String myUid, final String pId, final String pImage) {
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);

        //show delete menu only if the post is of current signed in user

        if (uid.equals(myUid)) {
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
        }
        popupMenu.getMenu().add(Menu.NONE, 2, 0, "View Details");
        //items click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == 0) {
                    //delete post
                    beginDelete(pId, pImage);
                } else if (id == 1) {
                    //Edit post
                    //start activity with key "editText" and id of the post clicked
                    Intent intent = new Intent(context, AddPostActivity.class);
                    intent.putExtra("key", "editPost");
                    intent.putExtra("editPostId", pId);
                    context.startActivity(intent);
                } else if (id == 2) {
                    //start PostDetailActivity
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId", pId);
                    context.startActivity(intent);
                }
                return false;
            }
        });
        //show menu
        popupMenu.show();
    }

    private void beginDelete(String pId, String pImage) {
        if (pImage.equals("noImage")) {
            //post is without image
            deleteWithoutImage(pId);
        } else {
            //post is with image
            deleteWithImage(pId, pImage);
        }
    }

    private void deleteWithImage(final String pId, String pImage) {
        //progress bar
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting Post...");
        pd.show();

        /*
         * 1)Delete Image url url
         * 2)Delete from database using post id*/
        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //image deleted ,now delete from database
                        Query fquery = FirebaseDatabase.getInstance().getReference("Posts")
                                .orderByChild("pId").equalTo(pId);
                        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    ds.getRef().removeValue();
                                }
                                //deleted 
                                Toast.makeText(context, "Post Deleted!", Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                pd.dismiss();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to delete
                        pd.dismiss();
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteWithoutImage(String pId) {
        //progress bar
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting Post...");
        pd.show();

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts")
                .orderByChild("pId").equalTo(pId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ds.getRef().removeValue();
                }
                //deleted
                Toast.makeText(context, "Post Deleted!", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                pd.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }


    class HolderPosts extends RecyclerView.ViewHolder {

        //views of row_posts.xml
        ImageView uPictureIv, pImageIv;
        TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn, commentBtn, shareBtn;
        LinearLayout profileLl;

        public HolderPosts(@NonNull View itemView) {
            super(itemView);

            //init views
            uPictureIv = itemView.findViewById(R.id.uPictureIv);
            pImageIv = itemView.findViewById(R.id.pImageIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            profileLl = itemView.findViewById(R.id.profileLl);
        }
    }
}

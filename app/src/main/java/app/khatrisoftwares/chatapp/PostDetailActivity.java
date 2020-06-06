package app.khatrisoftwares.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import app.khatrisoftwares.chatapp.adapters.AdapterComments;
import app.khatrisoftwares.chatapp.models.ModelComment;

public class PostDetailActivity extends AppCompatActivity {

    //to get details of user and post
    String myUid, myEmail, myName, myDp, postId, pLikes, hisDp, hisName;
    String hisUid, pImage;

    //progress bar
    ProgressDialog pd;

    //views
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
    ImageButton moreBtn;
    Button likeBtn, shareBtn;
    LinearLayout profileLl;
    RecyclerView commentsRv;

    List<ModelComment> commentList;
    AdapterComments adapterComments;

    //add comments views
    EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;

    boolean mProcessComment = false;
    boolean mProcessLike = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        getSupportActionBar().setTitle("Post Detail");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //get id of post using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        //init views
        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);
        profileLl = findViewById(R.id.profileLl);
        commentsRv = findViewById(R.id.commentsRv);


        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);

        loadPostInfo();

        checkUserStatus();

        loadUserInfo();

        setLikes();//set likes for each post

        getSupportActionBar().setSubtitle("SignedIn as: " + myEmail);

        loadComments();

        //send comment
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });

        //like btn click handle
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });

        //more btn click handle
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });

        //share btn click handle
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pTitle = pTitleTv.getText().toString().trim();
                String pDesc = pDescriptionTv.getText().toString().trim();

                /*Some posts contains only text ,and some contains both text and images ,here both cases are handled*/
                //get image from image view
                BitmapDrawable bitmapDrawable = (BitmapDrawable) pImageIv.getDrawable();

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

        pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PostDetailActivity.this, PostLikedActivity.class);
                intent.putExtra("postId",postId);
                startActivity(intent);
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

        if (!hisUid.equals( myUid)) {
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
        startActivity(Intent.createChooser(sIntent, "Share Via")); //msg to show in share dialog
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
        startActivity(Intent.createChooser(sIntent, "Share Via")); //msg to show in share dialog
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            imageFolder.mkdirs(); //create if not exists
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "app.khatrisoftwares.chatapp.fileprovider", file);
        } catch (Exception e) {
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void loadComments() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        commentsRv.setLayoutManager(layoutManager);

        //init comments
        commentList = new ArrayList<>();
        //path of the comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts")
                .child(postId).child("Comments");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelComment modelComment = ds.getValue(ModelComment.class);

                    commentList.add(modelComment);


                    adapterComments = new AdapterComments(getApplicationContext(), commentList, myUid, postId);
                    //set adapter to
                    commentsRv.setAdapter(adapterComments);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void showMoreOptions() {
        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        //show delete menu only if the post is of current signed in user

        if (hisUid.equals(myUid)) {
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");

            //items click listener
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int id = item.getItemId();
                    if (id == 0) {
                        //delete post
                        beginDelete();
                    } else if (id == 1) {
                        //Edit post
                        //start activity with key "editText" and id of the post clicked
                        Intent intent = new Intent(PostDetailActivity.this, AddPostActivity.class);
                        intent.putExtra("key", "editPost");
                        intent.putExtra("editPostId", postId);
                        startActivity(intent);
                    }
                    return false;
                }
            });
            //show menu
            popupMenu.show();
        }
    }

    private void beginDelete() {
        if (pImage.equals("noImage")) {
            //post is without image
            deleteWithoutImage();
        } else {
            //post is with image
            deleteWithImage();
        }
    }

    private void deleteWithoutImage() {
        //progress bar
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting Post...");
        pd.show();

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts")
                .orderByChild("pId").equalTo(postId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ds.getRef().removeValue();
                }
                //deleted
                Toast.makeText(PostDetailActivity.this, "Post Deleted!", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                pd.dismiss();
            }
        });
    }

    private void deleteWithImage() {
        //progress bar
        final ProgressDialog pd = new ProgressDialog(this);
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
                                .orderByChild("pId").equalTo(postId);
                        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    ds.getRef().removeValue();
                                }
                                //deleted
                                Toast.makeText(PostDetailActivity.this, "Post Deleted!", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(PostDetailActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void setLikes() {
        //when the details of the post is loading ,also check for whether current user has liked it or not
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("Likes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postId).hasChild(myUid)) {
                    //user has liked the post
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked_primary, 0, 0, 0);
                    likeBtn.setText("Liked");
                } else {
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                    likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void likePost() {

        mProcessLike = true;
        //get id of the post clicked
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("Likes");
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessLike) {
                    if (dataSnapshot.child(postId).hasChild(myUid)) {
                        //already liked so remove like
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) - 1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;
                    } else {
                        //not liked ,like post
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) + 1));
                        likesRef.child(postId).child(myUid).setValue("Liked");
                        mProcessLike = false;

                        addToHisNotifications("" + hisUid, "" + postId, "Liked your post");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void postComment() {
        pd = new ProgressDialog(this);
        pd.setMessage("Adding Comment...");


        //get data from comment edit text
        String comment = commentEt.getText().toString().trim();

        if (TextUtils.isEmpty(comment)) {
            Toast.makeText(this, "Comment is empty...", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        //each post will have a child "Comments" that will contain comments of that post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts")
                .child(postId).child("Comments");

        HashMap<String, Object> hashMap = new HashMap<>();
        //put info i hashmap
        hashMap.put("cId", timestamp);
        hashMap.put("comment", comment);
        hashMap.put("timestamp", timestamp);
        hashMap.put("uid", myUid);
        hashMap.put("uEmail", myEmail);
        hashMap.put("uDp", myDp);
        hashMap.put("uName", myName);

        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, "Comment Added...", Toast.LENGTH_SHORT).show();
                        commentEt.setText("");
                        updateCommentCount();

                        addToHisNotifications("" + hisUid, "" + postId, "Commented on your post");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                    }
                });
    }

    private void updateCommentCount() {
        mProcessComment = true;
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessComment) {
                    String comments = "" + dataSnapshot.child("pComments").getValue();
                    int newCommentVal = Integer.parseInt(comments) + 1;
                    ref.child("pComments").setValue("" + newCommentVal);
                    mProcessComment = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void loadUserInfo() {
        Query query = FirebaseDatabase.getInstance().getReference("Users");
        query.orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            myName = "" + ds.child("name").getValue();
                            myDp = "" + ds.child("image").getValue();

                            try {
                                Picasso.get().load(myDp).placeholder(R.drawable.ic_face_primary).into(cAvatarIv);
                            } catch (Exception e) {
//                                Picasso.get().load(R.drawable.ic_face_primary).into(cAvatarIv);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadPostInfo() {
        //get post using postId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //keep chaecking post untill it gets the post with the id same as postId
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String pTitle = "" + ds.child("pTitle").getValue();
                    String pDesc = "" + ds.child("pDesc").getValue();
                    pLikes = "" + ds.child("pLikes").getValue();
                    String pTimestamp = "" + ds.child("pTime").getValue();
                    pImage = "" + ds.child("pImage").getValue();
                    hisDp = "" + ds.child("uDp").getValue();
                    hisUid = "" + ds.child("uid").getValue();
                    String uEmail = "" + ds.child("uEmail").getValue();
                    hisName = "" + ds.child("uName").getValue();
                    String commentsCount = "" + ds.child("pComments").getValue();

                    //convert timestamp into proper format
                    //convert timestamp into dd/mm/yyyy hh:mm
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimestamp));
                    String pTimeFormat = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

                    //set data
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDesc);
                    pLikesTv.setText(pLikes + " Likes");
                    pTimeTv.setText(pTimeFormat);
                    uNameTv.setText(hisName);
                    pCommentsTv.setText(commentsCount + " Comments");

                    //set image the user who posted post
                    try {
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_face_primary).into(uPictureIv);
                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_face_primary).into(uPictureIv);
                    }

                    if (pImage.equals("noImage")) {
                        pImageIv.setVisibility(View.GONE);
                    } else {
                        pImageIv.setVisibility(View.VISIBLE);
                        try {
                            Picasso.get().load(pImage).into(pImageIv);
                        } catch (Exception e) {
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            //user is signed in
            myEmail = user.getEmail();
            myUid = user.getUid();
        } else {
            //user is not signed in
            startActivity(new Intent(PostDetailActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return super.onNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide some menu item
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}

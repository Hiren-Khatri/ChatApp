package app.khatrisoftwares.chatapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
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
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import app.khatrisoftwares.chatapp.R;

public class AddPostActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    DatabaseReference userDb;
    //views
    private EditText pTitleEt,pDescriptionEt;
    private ImageView pImageIv;
    private Button pUploadBtn;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //arrays request permission
    String[] cameraPermissions;
    String[] storagePermissions;

    String name, email, uid, dp;

    //info of post to be edited
    String editTitle,editDesc,editImage;

    //    uri of picked image
    private Uri image_uri = null;

    //progress bar
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        firebaseAuth = FirebaseAuth.getInstance();

        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //int request permission arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);

        checkUserStatus();

        //init views
        pTitleEt = findViewById(R.id.pTitleEt);
        pDescriptionEt = findViewById(R.id.pDescriptionEt);
        pImageIv = findViewById(R.id.pImageIv);
        pUploadBtn = findViewById(R.id.pUploadBtn);


        //get data though intent from previous activity adapter
        Intent intent = getIntent();

        //get data and its type from intent
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type!=null){
            if ("text/plain".equals(type)){
                //text data
                handleSendText(intent);
            } else if (type.startsWith("image")) {
                //image data
                handleSendImage(intent);
            }
        }

        final String isUpdateKey = ""+intent.getStringExtra("key");
        final String editPostId = ""+intent.getStringExtra("editPostId");

        //validate if we came here to update post i.e. came from adapter
        if (isUpdateKey.equals("editPost")){
            //update
            getSupportActionBar().setTitle("Update Post");
            pUploadBtn.setText("Update");
            loadPostData(editPostId);
        } else {
            getSupportActionBar().setTitle("Add New Post");
            pUploadBtn.setText("Upload");
        }

        getSupportActionBar().setSubtitle(email);


        //get info of current signed in user
        userDb = FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDb.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    name = ""+ds.child("name").getValue();
                    dp = ""+ds.child("image").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        //get image from camera or gallery on click
        pImageIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show image pick dialog
                showImagePicDialog();
            }
        });

        pUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get data (title,description) from editText
                String title = pTitleEt.getText().toString().trim();
                String description = pDescriptionEt.getText().toString().trim();

                if (TextUtils.isEmpty(title)){
                    Toast.makeText(AddPostActivity.this, "Enter title...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(description)){
                    Toast.makeText(AddPostActivity.this, "Enter description", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isUpdateKey.equals("editPost")){
                    beginUpdate(title,description,editPostId);
                } else {
                    uploadData(title,description);
                }
            }
        });
    }

    private void handleSendImage(Intent intent) {
        //handle the received image(uri)
        Uri imageUri = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri!=null){
            image_uri = imageUri;
            //set to image  view
            try {
                Picasso.get().load(image_uri).into(pImageIv);
            } catch (Exception e){

            }
        }
    }

    private void handleSendText(Intent intent) {
        //handle the received text
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText!=null){
            //set to description
            pDescriptionEt.setText(sharedText);
        }

    }

    private void beginUpdate(String title, String description, String editPostId) {
        progressDialog.setMessage("Updating Post....");
        progressDialog.show();

        if (!editImage.equals("noImage")){
            //was with image
            updateWithImage(title,description,editPostId);
        } else if (pImageIv.getDrawable()!=null){
            //was without image,but now has image
            updateWithNowImage(title,description,editPostId);
        } else {
            //was without image, and still no image in image view
            updateWithoutImage(title,description,editPostId);
        }
    }

    private void updateWithoutImage(String title, String description, String editPostId) {
        HashMap<String,Object> hashMap = new HashMap<>();
        //put post info
        hashMap.put("uid",uid);
        hashMap.put("uName",name);
        hashMap.put("uEmail",email);
        hashMap.put("uDp",dp);
        hashMap.put("pTitle",title);
        hashMap.put("pDesc",description);
        hashMap.put("pImage","noImage");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        ref.child(editPostId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPostActivity.this, "Updated...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateWithNowImage(final String title, final String description, final String editPostId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/"+"post_"+timestamp;

        //get image from imageview
        Bitmap bitmap = ((BitmapDrawable)pImageIv.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
        byte[] data = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image uploaded get its url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String downloadUri = uriTask.getResult().toString();
                        if (uriTask.isSuccessful()){
                            //uri is received upload to firebase
                            HashMap<String,Object> hashMap = new HashMap<>();
                            //put post info
                            hashMap.put("uid",uid);
                            hashMap.put("uName",name);
                            hashMap.put("uEmail",email);
                            hashMap.put("uDp",dp);
                            hashMap.put("pTitle",title);
                            hashMap.put("pDesc",description);
                            hashMap.put("pImage",downloadUri);

                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                            ref.child(editPostId)
                                    .updateChildren(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            progressDialog.dismiss();
                                            Toast.makeText(AddPostActivity.this, "Updated...", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressDialog.dismiss();
                                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateWithImage(final String title, final String description, final String editPostId) {
        StorageReference mPictureRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        mPictureRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //image deleted ,upload new image
                         String timestamp = String.valueOf(System.currentTimeMillis());
                        String filePathAndName = "Posts/"+"post_"+timestamp;

                        //get image from imageview
                        Bitmap bitmap = ((BitmapDrawable)pImageIv.getDrawable()).getBitmap();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        //image compress
                        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
                        byte[] data = baos.toByteArray();

                        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                        ref.putBytes(data)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        //image uploaded get its url
                                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                        while (!uriTask.isSuccessful());
                                        String downloadUri = uriTask.getResult().toString();
                                        if (uriTask.isSuccessful()){
                                            //uri is received upload to firebase
                                            HashMap<String,Object> hashMap = new HashMap<>();
                                            //put post info
                                            hashMap.put("uid",uid);
                                            hashMap.put("uName",name);
                                            hashMap.put("uEmail",email);
                                            hashMap.put("uDp",dp);
                                            hashMap.put("pTitle",title);
                                            hashMap.put("pDesc",description);
                                            hashMap.put("pImage",downloadUri);

                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                            ref.child(editPostId)
                                                    .updateChildren(hashMap)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(AddPostActivity.this, "Updated...", Toast.LENGTH_SHORT).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPostData(String editPostId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //get details of post using id of post
        Query fquery = reference.orderByChild("pId").equalTo(editPostId);
        fquery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    //get data
                    editTitle = ""+ds.child("pTitle").getValue();
                    editDesc = ""+ds.child("pDesc").getValue();
                    editImage = ""+ds.child("pImage").getValue();

                    //set data to views
                    pTitleEt.setText(editTitle);
                    pDescriptionEt.setText(editDesc);

                    //set image
                    if (!editImage.equals("noImage")){
                        try {
                            Picasso.get().load(editImage).into(pImageIv);
                        } catch (Exception e){

                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void uploadData(final String title, final String description) {
        progressDialog.setMessage("Publishing post....");
        progressDialog.show();

//        for post - image,name,post-id,post-publish-time
        final String timestamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/"+"post_"+timestamp;

        if (pImageIv.getDrawable() != null ){

            //get image from imageview
            Bitmap bitmap = ((BitmapDrawable)pImageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
            byte[] data = baos.toByteArray();

            //post with image
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            String downloadUri = uriTask.getResult().toString();

                            if (uriTask.isSuccessful()){
                                //uri is received upload post to firebase database
                                HashMap<Object,String> hashMap = new HashMap<>();
                                hashMap.put("uid",uid);
                                hashMap.put("uName",name);
                                hashMap.put("uEmail",email);
                                hashMap.put("uDp",dp);
                                hashMap.put("pId",timestamp);
                                hashMap.put("pTitle",title);
                                hashMap.put("pDesc",description);
                                hashMap.put("pImage",downloadUri);
                                hashMap.put("pTime",timestamp);
                                hashMap.put("pLikes","0");
                                hashMap.put("pComments","0");

                                //push to store post data
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                //put data
                                ref.child(timestamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressDialog.dismiss();
                                                Toast.makeText(AddPostActivity.this, "Post published", Toast.LENGTH_SHORT).show();
                                                //reset views
                                                pTitleEt.setText("");
                                                pDescriptionEt.setText("");
                                                pImageIv.setImageURI(null);
                                                image_uri = null;

                                                //send notification
                                                prepareNotification(
                                                        ""+timestamp,
                                                        ""+name+ "added new post",
                                                        ""+title+"\n"+description,
                                                        "PostNotification",
                                                        "POST");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            //post without image
            //uri is received upload post to firebase database
            HashMap<Object,String> hashMap = new HashMap<>();
            hashMap.put("uid",uid);
            hashMap.put("uName",name);
            hashMap.put("uEmail",email);
            hashMap.put("uDp",dp);
            hashMap.put("pId",timestamp);
            hashMap.put("pTitle",title);
            hashMap.put("pDesc",description);
            hashMap.put("pImage","noImage");
            hashMap.put("pTime",timestamp);
            hashMap.put("pLikes","0");
            hashMap.put("pComments","0");

            //push to store post data
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
            //put data
            ref.child(timestamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this, "Post published", Toast.LENGTH_SHORT).show();
                            //reset views
                            pTitleEt.setText("");
                            pDescriptionEt.setText("");
                            pImageIv.setImageURI(null);
                            image_uri = null;

                            //send notification
                            prepareNotification(
                                    ""+timestamp,
                                    ""+name+ " added new post",
                                    ""+title+"\n"+description,
                                    "PostNotification",
                                    "POST");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    private void prepareNotification(String pId,String title,String description,String notificationType,String notificationTopic){
        //prepare data for notification
        String NOTIFICATION_TOPIC = "/topics/" + notificationTopic;
        String NOTIFICATION_TITLE = title;//e.g abc uploaded new post
        String NOTIFICATION_MESSAGE = description;//content of post
        String NOTIFICATION_TYPE = notificationType;

        //prepare json what to send,and where to send
        JSONObject notificationJsonObject = new JSONObject();
        JSONObject notificationBodyJsonObject = new JSONObject();

        try {
            //what to send
            notificationBodyJsonObject.put("notificationType",NOTIFICATION_TYPE);
            notificationBodyJsonObject.put("sender",uid);//uid of current user
            notificationBodyJsonObject.put("pId",pId);
            notificationBodyJsonObject.put("pTitle",NOTIFICATION_TITLE);
            notificationBodyJsonObject.put("pDescription",NOTIFICATION_MESSAGE);
            //where to send
            notificationJsonObject.put("to",NOTIFICATION_TOPIC);
            notificationJsonObject.put("data",notificationBodyJsonObject);//data to be sent
        } catch (JSONException e) {
//            e.printStackTrace();
            Snackbar.make(findViewById(android.R.id.content),""+e.getMessage(),Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimaryDark)).show();
        }

        sendPostNotification(notificationJsonObject);
    }

    private void sendPostNotification(JSONObject notificationJsonObject) {
        //send volley object request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("FCM_RESPONSE","OnResponse: "+response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //error occurred
                        Log.d("FCM_RESPONSE_ERROR","OnResponse: "+error);

//                        Snackbar.make(findViewById(android.R.id.content),""+error.getMessage(),Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimaryDark)).show();
                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
//                    put required headers
                Map<String,String> headers = new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("Authorization","key=AAAAm3gR4P4:APA91bFg_g4-DWLCFSwBcv33atHYGGyRniykAtyndlntLyQsSlf8wD6JyXfc0MIZQWgzbrhv76oFqxxyg0jr2RQXa_-TSrKfAaLEywXSs9TlMiAzml7L9iT-p91ESgLrEoXbWk9cJdgt");
                return headers;
            }
        };
        //enqueue the volley request
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();
    }

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user!=null){
            //user signed in here
            email = user.getEmail();
            uid = user.getUid();
        } else {
            //user not signed in,go to main activity
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showImagePicDialog() {
//show dialog
        String[] options = {"Camera", "Gallery"};

        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image From");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item click
                if (which == 0) {
//                   Camera clicked
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }

                } else if (which == 1) {
                    //Gallery clicked
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }
                }
            }
        });
        builder.create().show();
    }

    private void pickFromCamera() {
        //Intent for picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");

        //put image uri
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);


//        intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        //pick image from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PICK_GALLERY_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        requestPermissions( storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        pickFromCamera();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "Please grant camera and storage permissions!", Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimaryDark)).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickFromGallery();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "Please grant storage permission!", Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimaryDark)).show();
                    }
                }
            }
            break;

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //image picked from gallery
                image_uri = data.getData();

//                uploadProfileCoverPhoto(image_uri);
                pImageIv.setImageURI(image_uri);
            }
            else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image picked from camera

//                uploadProfileCoverPhoto(image_uri);
                pImageIv.setImageURI(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}

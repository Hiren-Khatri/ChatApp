package app.khatrisoftwares.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class GroupCreateActivity extends AppCompatActivity {

    private ActionBar actionBar;
    private FirebaseAuth firebaseAuth;
    private ImageView groupIconIv;
    private EditText groupTitleEt, groupDescriptionEt;
    private FloatingActionButton createGroupBtn;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //arrays request permission
    String[] cameraPermissions;
    String[] storagePermissions;

    //    uri of picked image
    private Uri image_uri;

    private ProgressDialog progressDialog;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_create);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Create Group");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        //int request permission arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        groupIconIv = findViewById(R.id.groupIconIv);
        groupTitleEt = findViewById(R.id.groupTitleEt);
        groupDescriptionEt = findViewById(R.id.groupDescriptionEt);
        createGroupBtn = findViewById(R.id.createGroupBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();

        groupIconIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePicDialog();
            }
        });

        createGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCreatingGroup();
            }
        });

    }

    private void startCreatingGroup() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Creating Group");

        //input title,description
        final String groupTitle = groupTitleEt.getText().toString().trim();
        final String groupDescription = groupDescriptionEt.getText().toString().trim();

        if (TextUtils.isEmpty(groupTitle)) {
            groupTitleEt.setError("required");
            return;
        }

        final String g_timestamp = "" + System.currentTimeMillis();

        progressDialog.show();

        if (image_uri == null) {
            //creating group without image
            createGroup(
                    "" + g_timestamp,
                    "" + groupTitle,
                    "" + groupDescription,
                    ""
            );
        } else {
            //creating group with icon
            String fileNameAndPath = "Group_Imgs/" + "image" + g_timestamp;
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileNameAndPath);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task<Uri> p_uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!p_uriTask.isSuccessful()) ;

                            Uri p_downloadUri = p_uriTask.getResult();
                            if (p_uriTask.isSuccessful()) {
                                createGroup(
                                        "" + g_timestamp,
                                        "" + groupTitle,
                                        "" + groupDescription,
                                        "" + p_downloadUri
                                );
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Snackbar.make(findViewById(android.R.id.content), "" + e.getMessage(), Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimaryDark)).show();
                        }
                    });
        }
    }

    private void createGroup(final String g_timestamp, String gTitle, String gDesc, String icon) {
        final HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("groupId", g_timestamp);
        hashMap.put("groupTitle", gTitle);
        hashMap.put("groupDescription", gDesc);
        hashMap.put("groupIcon", "" + icon);
        hashMap.put("timestamp", "" + g_timestamp);
        hashMap.put("createdBy", "" + firebaseAuth.getUid());

        //creating group
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(g_timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //group created
                        //setup member info (add current user in group's participants list
                        HashMap<String,String> hashMap1 = new HashMap<>();
                        hashMap1.put("uid",firebaseAuth.getUid());
                        hashMap1.put("role","creator");
                        hashMap1.put("timestamp",g_timestamp);

                        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Groups");
                        ref1.child(g_timestamp).child("Participants").child(firebaseAuth.getUid())
                                .setValue(hashMap1)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //participant added
                                        progressDialog.dismiss();
                                        Snackbar.make(findViewById(android.R.id.content), "Group Created...", Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimaryDark)).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Snackbar.make(findViewById(android.R.id.content), "" + e.getMessage(), Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimaryDark)).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Snackbar.make(findViewById(android.R.id.content), "" + e.getMessage(), Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimaryDark)).show();
                    }
                });
    }

    private void checkUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            actionBar.setSubtitle(currentUser.getEmail());
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
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
        values.put(MediaStore.Images.Media.TITLE, "Group Image Icon Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Group Image Icon Description");

        //put image uri
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);


//        intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        //pick image from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
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
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //image picked from gallery
                image_uri = data.getData();

//                uploadProfileCoverPhoto(image_uri);
                groupIconIv.setImageURI(image_uri);
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //image picked from camera

//                uploadProfileCoverPhoto(image_uri);
                groupIconIv.setImageURI(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}

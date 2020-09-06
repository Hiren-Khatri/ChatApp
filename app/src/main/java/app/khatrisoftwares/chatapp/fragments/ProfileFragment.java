package app.khatrisoftwares.chatapp.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.khatrisoftwares.chatapp.activities.AddPostActivity;
import app.khatrisoftwares.chatapp.activities.MainActivity;
import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.activities.SettingsActivity;
import app.khatrisoftwares.chatapp.adapters.AdapterPosts;
import app.khatrisoftwares.chatapp.models.ModelPost;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {
    //firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private DatabaseReference ref;
    private StorageReference storageReference;
    private String storagePath = "Users_Profile_Cover_Images/";

    //views of xml file
    private ImageView avatarIv, coverIv;
    private TextView nameTv, emailTv, phoneTv;
    private FloatingActionButton fab;
    private RecyclerView postsRv;

    private ProgressDialog progressDialog;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //arrays request permission
    String[] cameraPermissions;
    String[] storagePermissions;

    //    uri of picked image
    private Uri image_uri;

    private String profileOrCoverPhoto;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        ref = FirebaseDatabase.getInstance().getReference("Users");

        //int request permission arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        avatarIv = view.findViewById(R.id.avatarIv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        fab = view.findViewById(R.id.fab);
        postsRv = view.findViewById(R.id.postsRv);

        progressDialog = new ProgressDialog(getActivity());


        ref.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String email = dataSnapshot.child("email").getValue().toString();
                String phone = dataSnapshot.child("phone").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String cover = dataSnapshot.child("cover").getValue().toString();

                nameTv.setText(name);
                emailTv.setText(email);
                phoneTv.setText(phone);
                try {
                    Picasso.get().load(image).into(avatarIv);
                } catch (Exception e) {
                    avatarIv.setImageResource(R.drawable.ic_face_white);
                }
                try {
                    Picasso.get().load(cover).into(coverIv);
                } catch (Exception e) {
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        postList = new ArrayList<>();

        checkUserStatus();
        loadMyPosts();

        return view;
    }

    private void loadMyPosts() {
        //linear layout for postsRv
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to postsRv
        postsRv.setLayoutManager(layoutManager);

        //init post list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load my posts
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from db
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelPost myPost = ds.getValue(ModelPost.class);

                    postList.add(myPost);

                    adapterPosts = new AdapterPosts(getActivity(),postList,true);
                    //set adapter to postRv
                    postsRv.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
//                if (getActivity()!=null){
//                    Toast.makeText(getActivity(), ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
//                }

            }
        });
    }

    private void searchMyPosts(final String searchQuery) {
        //linear layout for postsRv
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to postsRv
        postsRv.setLayoutManager(layoutManager);

        //init post list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load my posts
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from db
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelPost myPost = ds.getValue(ModelPost.class);


                    if (myPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) || myPost.getpDesc().toLowerCase().contains(searchQuery.toLowerCase())){
                        postList.add(myPost);
                    }

                    adapterPosts = new AdapterPosts(getActivity(),postList,true);
                    //set adapter to postRv
                    postsRv.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void showEditProfileDialog() {
        //show dialog
        String options[] = {"Edit Profile Picture", "Edit Cover Picture", "Edit Name", "Edit Phone","Change Password"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Action");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item click
                if (which == 0) {
//                    Edit profile picture clicked
                    progressDialog.setMessage("Updating Profile Picture...");
                    profileOrCoverPhoto = "image";
                    showImagePicDialog();
                } else if (which == 1) {
                    //edit cover photo clicked
                    progressDialog.setMessage("Updating Cover Picture...");
                    profileOrCoverPhoto = "cover";
                    showImagePicDialog();
                } else if (which == 2) {
                    //edit name clicked
                    progressDialog.setMessage("Updating Name...");
                    showNamePhoneUpdateDialog("name");
                } else if (which == 3) {
                    //edit phone clicked
                    progressDialog.setMessage("Updating Phone...");
                    showNamePhoneUpdateDialog("phone");
                } else if (which == 4){
                    progressDialog.setMessage("Changing Password...");
                    showChangePasswordDialog();
                }
            }
        });
        builder.create().show();
    }

    private void showChangePasswordDialog() {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_update_password,null);
        final EditText passwordEt = view.findViewById(R.id.passwordEt);
        final EditText newPasswordEt = view.findViewById(R.id.newPasswordEt);
        Button updatePasswordBtn = view.findViewById(R.id.updatePasswordBtn);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.show();


        updatePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate data
                String oldPass = passwordEt.getText().toString().trim();
                String newPass = newPasswordEt.getText().toString().trim();
                if (TextUtils.isEmpty(oldPass)){
                    passwordEt.setError("Current password required");
                    return;
                }
                if (newPass.length()<6){
                    newPasswordEt.setError("Password length must atleast 6 characters");
                    return;
                }
                dialog.dismiss();
                updatePassword(oldPass,newPass);
            }
        });

    }

    private void updatePassword(String oldPass, final String newPass) {
        progressDialog.show();

        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(),oldPass);
        user.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //successfully authenticated
                        user.updatePassword(newPass)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        progressDialog.dismiss();
                                        Snackbar.make(getView(),"Password Updated...",Snackbar.LENGTH_SHORT).setBackgroundTint(getContext().getColor(R.color.colorPrimaryDark)).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Snackbar.make(getView(),""+e.getMessage(),Snackbar.LENGTH_SHORT).setBackgroundTint(getContext().getColor(R.color.colorPrimaryDark)).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Snackbar.make(getView(),""+e.getMessage(),Snackbar.LENGTH_SHORT).setBackgroundTint(getContext().getColor(R.color.colorPrimaryDark)).show();
                    }
                });
    }

    private void showNamePhoneUpdateDialog(final String key) {
        //custom dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update "+key);
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);

        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter "+key);
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String value = editText.getText().toString().trim();
                        if (TextUtils.isEmpty(value)){
                            editText.setError("required");
                            Snackbar.make(getActivity().findViewById(android.R.id.content),"Please enter "+key,Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
                        } else {
                            progressDialog.show();
                            HashMap<String,Object> hashMap = new HashMap<>();
                            hashMap.put(key,value);
                            ref.child(user.getUid()).updateChildren(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            progressDialog.dismiss();
                                            Snackbar.make(getActivity().findViewById(android.R.id.content),"Updated...",Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressDialog.dismiss();
                                            Snackbar.make(getActivity().findViewById(android.R.id.content),""+e.getMessage(),Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
                                        }
                                    });
                            //if user update his name ,also change it from his posts
                            if (key.equals("name")){
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                Query query = ref.orderByChild("uid").equalTo(uid);
                                query.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds : dataSnapshot.getChildren()){
                                            String child = ds.getKey();
                                            dataSnapshot.getRef().child(child).child("uName").setValue(value);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                                //update name in comments
                                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                                            String child = ds.getKey();
                                            if (dataSnapshot.child(child).hasChild("Comments")){
                                                String child1 = ""+dataSnapshot.child(child).getKey();
                                                Query child2 = FirebaseDatabase.getInstance().getReference("Posts")
                                                        .child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                                child2.addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                                                            String child = ds.getKey();
                                                            dataSnapshot.getRef().child(child).child("uName").setValue(value);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

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
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.create().show();
    }

    private void showImagePicDialog() {
//show dialog
        String options[] = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        requestPermissions( storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
                        Snackbar.make(getActivity().findViewById(android.R.id.content), "Please grant camera and storage permissions!", Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
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
                        Snackbar.make(getActivity().findViewById(android.R.id.content), "Please grant storage permission!", Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
                    }
                }
            }
            break;

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void pickFromCamera() {
        //Intent for picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");

        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);


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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //image picked from gallery
                image_uri = data.getData();

                uploadProfileCoverPhoto(image_uri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image picked from camera

                uploadProfileCoverPhoto(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(Uri uri) {
        progressDialog.show();
        String filePathAndName = storagePath+""+profileOrCoverPhoto+"_"+user.getUid();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        storageReference.child(filePathAndName).putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        final Uri downloadUri = uriTask.getResult();

                        if (uriTask.isSuccessful()){
                            HashMap<String,Object> hashMap = new HashMap<>();
                            hashMap.put(profileOrCoverPhoto,downloadUri.toString());

                            ref.child(user.getUid()).updateChildren(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            progressDialog.dismiss();
                                            Snackbar.make(getActivity().findViewById(android.R.id.content),"Image updated...",Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressDialog.dismiss();
                                            Snackbar.make(getActivity().findViewById(android.R.id.content), ""+e.getMessage(), Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
                                        }
                                    });

                            //if user update his name ,also change it from his posts
                            if (profileOrCoverPhoto.equals("image")){
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                Query query = ref.orderByChild("uid").equalTo(uid);
                                query.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds : dataSnapshot.getChildren()){
                                            String child = ds.getKey();
                                            dataSnapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                //update user image in current comments
                                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                                            String child = ds.getKey();
                                            if (dataSnapshot.child(child).hasChild("Comments")){
                                                String child1 = ""+dataSnapshot.child(child).getKey();
                                                Query child2 = FirebaseDatabase.getInstance().getReference("Posts")
                                                        .child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                                child2.addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                                                            String child = ds.getKey();
                                                            dataSnapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

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

                        } else {
                            progressDialog.dismiss();
                            Snackbar.make(getActivity().findViewById(android.R.id.content),"Something went wrong please try again",Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Snackbar.make(getActivity().findViewById(android.R.id.content), ""+e.getMessage(), Snackbar.LENGTH_SHORT).setBackgroundTint(getActivity().getColor(R.color.colorPrimaryDark)).show();
                    }
                });
    }

    public void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user!=null){
            //user signed in here
            uid = user.getUid();
        } else {
            //user not signed in,go to main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);//to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_main,menu);

        menu.findItem(R.id.action_create_group).setVisible(false);

        MenuItem item = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //called when user submits
                if (!TextUtils.isEmpty(query)){
                    searchMyPosts(query);
                } else {
                    loadMyPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //called while user is typing
                if (!TextUtils.isEmpty(newText)){
                    searchMyPosts(newText);
                } else {
                    loadMyPosts();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu,menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        if (item.getItemId() == R.id.action_add_post){
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        } else if (item.getItemId()==R.id.action_settings){
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}

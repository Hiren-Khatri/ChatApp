package app.khatrisoftwares.chatapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.adapters.AdapterPosts;
import app.khatrisoftwares.chatapp.models.ModelPost;

public class ThereProfileActivity extends AppCompatActivity {

    //views of xml file
    private ImageView avatarIv, coverIv;
    private TextView nameTv, emailTv, phoneTv;
    private RecyclerView postsRv;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    FirebaseAuth firebaseAuth;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);


        getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        avatarIv =findViewById(R.id.avatarIv);
        coverIv = findViewById(R.id.coverIv);
        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        phoneTv = findViewById(R.id.phoneTv);
        postsRv = findViewById(R.id.postsRv);

        postsRv = findViewById(R.id.postsRv);

        firebaseAuth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance().getReference("Users");

        //get uid
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");

        ref.child(uid).addValueEventListener(new ValueEventListener() {
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

        postList = new ArrayList<>();

        checkUserStatus();

        loadHisPosts();
    }

    private void loadHisPosts() {
        //linear layout for postsRv
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
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

                    adapterPosts = new AdapterPosts(ThereProfileActivity.this,postList,true);
                    //set adapter to postRv
                    postsRv.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ThereProfileActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void searchHisPosts(final String searchQuery) {
        //linear layout for postsRv
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
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

                    adapterPosts = new AdapterPosts(ThereProfileActivity.this,postList,true);
                    //set adapter to postRv
                    postsRv.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ThereProfileActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user!=null){
            //user signed in here
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
        menu.findItem(R.id.action_add_post).setVisible(false);//hide add post
        menu.findItem(R.id.action_create_group).setVisible(false);

        MenuItem item = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //called when user submits
                if (!TextUtils.isEmpty(query)){
                    searchHisPosts(query);
                } else {
                    loadHisPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //called while user is typing
                if (!TextUtils.isEmpty(newText)){
                    searchHisPosts(newText);
                } else {
                    loadHisPosts();
                }
                return false;
            }
        });
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
}

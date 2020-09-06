package app.khatrisoftwares.chatapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.adapters.AdapterUsers;
import app.khatrisoftwares.chatapp.models.ModelUser;

public class PostLikedActivity extends AppCompatActivity {
    String postId;
    RecyclerView likedRv;

    private FirebaseAuth firebaseAuth;

    private List<ModelUser> userList;
    private AdapterUsers adapterUsers;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_liked);

        getSupportActionBar().setTitle("Post Liked By");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();

        getSupportActionBar().setSubtitle(firebaseAuth.getCurrentUser().getEmail());

        likedRv = findViewById(R.id.likedRv);

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        userList = new ArrayList<>();
        DatabaseReference  ref = FirebaseDatabase.getInstance().getReference("Likes");
        ref.child(postId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    String hisUid = ""+ds.getRef().getKey();

                    //get user info from each id
                    getUsers(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getUsers(String hisUid) {
        //get information of each user using uid
        //get user info from hisUid
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
        userRef.orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            ModelUser user = ds.getValue(ModelUser.class);
                            userList.add(user);
                        }
                        adapterUsers = new AdapterUsers(PostLikedActivity.this,userList);
                        likedRv.setAdapter(adapterUsers);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}

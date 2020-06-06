package app.khatrisoftwares.chatapp.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import app.khatrisoftwares.chatapp.GroupCreateActivity;
import app.khatrisoftwares.chatapp.MainActivity;
import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.SettingsActivity;
import app.khatrisoftwares.chatapp.adapters.AdapterUsers;
import app.khatrisoftwares.chatapp.models.ModelUser;


/**
 * A simple {@link Fragment} subclass.
 */
public class UsersFragment extends Fragment {
    private RecyclerView usersRv;
    private AdapterUsers adapterUsers;
    private List<ModelUser> userList;
    private FirebaseAuth firebaseAuth;



    public UsersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_users, container, false);
        usersRv = view.findViewById(R.id.usersRv);
        usersRv.setHasFixedSize(true);

        firebaseAuth = FirebaseAuth.getInstance();

        getAllUsers();

        return view;
    }

    private void getAllUsers() {
        userList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);
                    if (!modelUser.getUid().equals(FirebaseAuth.getInstance().getUid())){
                        userList.add(modelUser);
                    }
                }
                adapterUsers = new AdapterUsers(getActivity(),userList);
                usersRv.setAdapter(adapterUsers);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchUsers(final String query) {
        userList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);

                    //get all searched user
                    if (!modelUser.getUid().equals(firebaseAuth.getUid())){

                        if (modelUser.getName().toLowerCase().contains(query.toLowerCase()) ||
                                modelUser.getEmail().toLowerCase().contains(query.toLowerCase())){
                            userList.add(modelUser);
                        }

                    }
                }
                adapterUsers = new AdapterUsers(getActivity(),userList);
                adapterUsers.notifyDataSetChanged();
                usersRv.setAdapter(adapterUsers);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user!=null){
            //user signed in here
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

        //hide addpost menu from this fragment
        menu.findItem(R.id.action_add_post).setVisible(false);

        //serachview
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //called when user press search button from keyboard
                //if search query is not empty then search
                if (!TextUtils.isEmpty(query.trim())){
                    searchUsers(query);
                } else {
                    getAllUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //called when user press search button from keyboard
                //if search query is not empty then search
                if (!TextUtils.isEmpty(newText.trim())){
                    searchUsers(newText);
                } else {
                    getAllUsers();
                }
                return false;            }
        });

        super.onCreateOptionsMenu(menu,menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        } else if (item.getItemId()==R.id.action_settings){
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }  else if (item.getItemId()==R.id.action_create_group){
            startActivity(new Intent(getActivity(), GroupCreateActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}

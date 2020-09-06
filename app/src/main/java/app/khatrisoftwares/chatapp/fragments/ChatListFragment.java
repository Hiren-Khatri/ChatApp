package app.khatrisoftwares.chatapp.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

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

import app.khatrisoftwares.chatapp.activities.GroupCreateActivity;
import app.khatrisoftwares.chatapp.activities.MainActivity;
import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.activities.SettingsActivity;
import app.khatrisoftwares.chatapp.adapters.AdapterChatlist;
import app.khatrisoftwares.chatapp.models.ModelChat;
import app.khatrisoftwares.chatapp.models.ModelChatlist;
import app.khatrisoftwares.chatapp.models.ModelUser;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatListFragment extends Fragment {
    private FirebaseAuth firebaseAuth;

    private RecyclerView chatlistRv;
    List<ModelChatlist> chatlistList;
    List<ModelUser> userList;
    DatabaseReference ref;
    FirebaseUser currentUser;
    AdapterChatlist adapterChatlist;
    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        chatlistRv = view.findViewById(R.id.chatlistRv);

        chatlistList = new ArrayList<>();

        ref = FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUser.getUid());
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatlistList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChatlist chatlist = ds.getValue(ModelChatlist.class);
                    chatlistList.add(chatlist);
                }

                loadChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return view;
    }

    private void loadChats() {
        userList = new ArrayList<>();
        ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelUser user = ds.getValue(ModelUser.class);
                    for (ModelChatlist chatlist : chatlistList) {
                        if (user.getUid() != null && user.getUid().equals(chatlist.getId())) {
                                userList.add(user);
                                break;
                        }
                    }
                    //adapter
                    adapterChatlist = new AdapterChatlist(getContext(),userList);
                    //set adapter
                    chatlistRv.setAdapter(adapterChatlist);

                    for (int i=0;i<userList.size();i++){
                        lastMessage(userList.get(i).getUid());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void lastMessage(final String userId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String lastMessage = "default";
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat==null){
                        continue;
                    }
                    String sender = chat.getSender();
                    String receiver = chat.getReceiver();
                    if (sender == null || receiver == null){
                        continue;
                    }
                    if (chat.getReceiver().equals(currentUser.getUid()) && chat.getSender().equals(userId)
                            ||chat.getReceiver().equals(userId) && chat.getSender().equals(currentUser.getUid())){
                        if (chat.getType().equals("image")){
                            lastMessage = "Sent a photo";
                        } else {
                            lastMessage = chat.getMessage();
                        }
                    }
                }

                adapterChatlist.setLastMessageMap(userId,lastMessage);
                adapterChatlist.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
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
        menuInflater.inflate(R.menu.menu_main, menu);

        //hide addpost menu from this fragment
        menu.findItem(R.id.action_add_post).setVisible(false);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
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

package app.khatrisoftwares.chatapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.fragments.ChatListFragment;
import app.khatrisoftwares.chatapp.fragments.GroupChatsFragment;
import app.khatrisoftwares.chatapp.fragments.HomeFragment;
import app.khatrisoftwares.chatapp.fragments.NotificationsFragment;
import app.khatrisoftwares.chatapp.fragments.ProfileFragment;
import app.khatrisoftwares.chatapp.fragments.UsersFragment;
import app.khatrisoftwares.chatapp.notifications.Token;

public class DashboardActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private BottomNavigationView navigationView;

    String mUID;

    private int REQUEST_CODE_BATTERY_OPTIMIZATIONS = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        getSupportActionBar().setTitle("Profile");


        firebaseAuth = FirebaseAuth.getInstance();

        navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);
        //home fragment transaction
        getSupportActionBar().setTitle("Home");//actionbar title
        HomeFragment homeFragment = new HomeFragment();
        FragmentTransaction ftl = getSupportFragmentManager().beginTransaction();
        ftl.add(R.id.content,homeFragment,"").commit();

        checkUserStatus();
        checkForBatteryOptimizations();

    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }

    public void updateToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(mUID).setValue(mToken);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    //handling item click
                    switch (item.getItemId()){
                        case R.id.nav_home:
                            //home fragment transaction
                            getSupportActionBar().setTitle("Home");//actionbar title
                            HomeFragment homeFragment = new HomeFragment();
                            FragmentTransaction ftl = getSupportFragmentManager().beginTransaction();
                            ftl.replace(R.id.content,homeFragment,"").commit();
                            return true;
                        case R.id.nav_profile:
                            //profile fragment transaction
                            getSupportActionBar().setTitle("Profile");//actionbar title
                            ProfileFragment profileFragment = new ProfileFragment();
                            FragmentTransaction ftl1 = getSupportFragmentManager().beginTransaction();
                            ftl1.replace(R.id.content,profileFragment,"").commit();
                            return true;
                        case R.id.nav_users:
                            //users fragment transaction
                            getSupportActionBar().setTitle("Users");//actionbar title
                            UsersFragment usersFragment = new UsersFragment();
                            FragmentTransaction ftl2 = getSupportFragmentManager().beginTransaction();
                            ftl2.replace(R.id.content,usersFragment,"").commit();
                            return true;
                        case R.id.nav_chat:
                            //users fragment transaction
                            getSupportActionBar().setTitle("Chats");//actionbar title
                            ChatListFragment chatListFragment = new ChatListFragment();
                            FragmentTransaction ftl3 = getSupportFragmentManager().beginTransaction();
                            ftl3.replace(R.id.content,chatListFragment,"").commit();
                            return true;
                        case R.id.nav_more:
                           showMoreOptions();
                            return true;
                    }
                    return false;
                }
            };

    private void showMoreOptions() {
        //pop menu
        PopupMenu popupMenu = new PopupMenu(this,navigationView, Gravity.END);
        //item to show in menu
        popupMenu.getMenu().add(Menu.NONE,0,0,"Notifications");
        popupMenu.getMenu().add(Menu.NONE,1,0,"Group Chats");

        //menu clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
               int id = item.getItemId();
               if (id == 0){
                   //notifications
                   getSupportActionBar().setTitle("Notifications");//actionbar title
                   NotificationsFragment notificationsFragment = new NotificationsFragment();
                   FragmentTransaction ftl4 = getSupportFragmentManager().beginTransaction();
                   ftl4.replace(R.id.content,notificationsFragment,"").commit();

               } else if (id == 1){
                   //group chat
                   getSupportActionBar().setTitle("Group Chats");//actionbar title
                   GroupChatsFragment groupChatsFragment = new GroupChatsFragment();
                   FragmentTransaction ftl5 = getSupportFragmentManager().beginTransaction();
                   ftl5.replace(R.id.content,groupChatsFragment,"").commit();
               }
                return false;
            }
        });
        popupMenu.show();
    }

    public void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user!=null){
            //user signed in here
            mUID = user.getUid();

            //save uid of currently signed in user in shared preferences
            SharedPreferences sp = getSharedPreferences("SP_USER",MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID",mUID);
            editor.apply();

            //update token
            updateToken(FirebaseInstanceId.getInstance().getToken());
        } else {
            //user not signed in,go to main activity
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    private  void checkForBatteryOptimizations(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("Battery optimization is enabled.It can interrupt running background service");
                builder.setPositiveButton("Disable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivityForResult(intent,REQUEST_CODE_BATTERY_OPTIMIZATIONS);
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.create().show();
            }else{
                checkForDrawOverOtherApps();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkForDrawOverOtherApps(){
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
            builder.setTitle("Warning");
            builder.setMessage("Draw over other apps permission is needed.It can interrupt running background service");
            builder.setNeutralButton("Go to Settings",(dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            });
            builder.create().show();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATIONS){
            checkForBatteryOptimizations();
        }
    }

}

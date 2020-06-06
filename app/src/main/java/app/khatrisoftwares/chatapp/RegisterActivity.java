package app.khatrisoftwares.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    //views
    private EditText emailEt,passwordEt;
    private Button registerBtn;
    private TextView haveAccountTv;

    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //init views
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        registerBtn = findViewById(R.id.registerBtn);
        haveAccountTv = findViewById(R.id.have_accountTv);

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering User...");

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEt.getText().toString().trim();
                String password = passwordEt.getText().toString().trim();

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    emailEt.setError("Invalid Email");
                    emailEt.requestFocus();
                }
                else if (password.length()<6){
                    passwordEt.setError("Password should be at least 6 characters long");
                    passwordEt.requestFocus();
                } else {
                    // Check if no view has focus:
                    View view = RegisterActivity.this.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    registerUser(email,password);
                }
            }
        });

        haveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser(String email, String password) {
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        progressDialog.dismiss();
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        String email = user.getEmail();
                        String uid = user.getUid();
                        HashMap<Object,String> hashMap = new HashMap<>();
                        hashMap.put("email",email);
                        hashMap.put("uid",uid);
                        hashMap.put("name","");
                        hashMap.put("onlineStatus","online");
                        hashMap.put("typingTo","noOne");
                        hashMap.put("phone","");
                        hashMap.put("image","");
                        hashMap.put("cover","");
                        //storing data to db
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                        ref.child(uid).setValue(hashMap);

                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),"Registered...\n"+user.getEmail(),Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                       Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),""+e.getMessage(),Snackbar.LENGTH_SHORT);
                       snackbar.setBackgroundTint(getColor(R.color.colorPrimary));
                       snackbar.show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}

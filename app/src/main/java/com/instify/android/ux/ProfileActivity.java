package com.instify.android.ux;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instify.android.R;
import com.instify.android.helpers.UserData;

public class ProfileActivity extends AppCompatActivity {

    DatabaseReference dbRef, userRef;
    FirebaseUser currentUser;
    EditText name, regno, sec, dept, year;
    Button editBtn, saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        dbRef =  FirebaseDatabase.getInstance().getReference();
        userRef = dbRef.child("users").child(currentUser.getUid());

        name = (EditText) findViewById(R.id.profile_name_et);
        regno = (EditText) findViewById(R.id.profile_regno_et);
        sec = (EditText) findViewById(R.id.profile_section_et);
        dept = (EditText) findViewById(R.id.profile_dept_et);
        year = (EditText) findViewById(R.id.profile_year_et);

        editBtn = (Button) findViewById(R.id.profile_edit_btn);
        saveBtn = (Button) findViewById(R.id.profile_save_btn);

        name.setEnabled(false);
        regno.setEnabled(false);
        sec.setEnabled(false);
        dept.setEnabled(false);
        year.setEnabled(false);

        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                name.setEnabled(true);
                regno.setEnabled(true);
                sec.setEnabled(true);
                dept.setEnabled(true);
                year.setEnabled(true);
                editBtn.setVisibility(View.INVISIBLE);
                saveBtn.setVisibility(View.VISIBLE);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                name.setEnabled(false);
                regno.setEnabled(false);
                sec.setEnabled(false);
                dept.setEnabled(false);
                year.setEnabled(false);
                editBtn.setVisibility(View.VISIBLE);
                saveBtn.setVisibility(View.INVISIBLE);
            }
        });

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserData data = dataSnapshot.getValue(UserData.class);
                regno.setText(data.regno);
                sec.setText(data.section);
                dept.setText(data.dept);
                year.setText(data.year + "");

                editBtn.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
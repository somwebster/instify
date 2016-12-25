package com.instify.android.ux.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instify.android.R;
import com.instify.android.upload.UploadNews;
import com.instify.android.ux.adapters.CampNewsAdapter;

/**
 * Created by Abhish3k on 2/23/2016.
 */

public class CampNewsFragment extends Fragment {

    public CampNewsFragment(){}

    public static CampNewsFragment newInstance(){
        CampNewsFragment frag = new CampNewsFragment();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    RecyclerView recyclerView;

    // Firebase definitions //
    DatabaseReference dbRef, campusRef;
    FirebaseRecyclerAdapter<CampusNewsData, CampusViewHolder> fAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_campus_news, container, false);
        //((ActivityMain) getActivity()).showFloatingActionButton();

        // Recycler view set up //
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_campus_news);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        // Firebase database setup //
        dbRef = FirebaseDatabase.getInstance().getReference();
        campusRef = dbRef.child("CampusNews");
        fAdapter = new FirebaseRecyclerAdapter<CampusNewsData, CampusViewHolder>(
                CampusNewsData.class, R.layout.card_view_campus, CampusViewHolder.class, campusRef) {
            @Override
            protected void populateViewHolder(final CampusViewHolder viewHolder, CampusNewsData model, int position) {
                viewHolder.campusTitle.setText(model.title);
                viewHolder.campusDescription.setText(model.description);
            }
        };

        recyclerView.setAdapter(fAdapter);

        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Click action
                Intent i = new Intent(getActivity(), UploadNews.class);
                startActivity(i);
            }
        });
        return rootView;
    }

    private static class CampusViewHolder extends RecyclerView.ViewHolder{
            TextView campusTitle, campusDescription;
        public CampusViewHolder(View v){
            super(v);
            campusTitle = (TextView) v.findViewById(R.id.campusTitle);
            campusDescription = (TextView) v.findViewById(R.id.campusDescription);
        }
    }

}


class CampusNewsData {

    public String title, description;

    CampusNewsData(){}

    CampusNewsData(CampusNewsData snap){
        this.title = snap.title;
        this.description = snap.description;
    }

    CampusNewsData(TextView t, TextView d) {
        this.title = t.getText().toString();
        this.description = d.getText().toString();
    }
}
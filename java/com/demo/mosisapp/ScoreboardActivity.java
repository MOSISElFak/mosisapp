package com.demo.mosisapp;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ScoreboardActivity extends AppCompatActivity
{
    private String TAG = "ScoreboardActivity";
    private ScoreboardListAdapter mListAdapter;
    private ListView mlistView;

    //private ProgressBar mProgressBar;

    private DatabaseReference refUsers;
    private Query scoreQuery;
    private ValueEventListener mValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        catch (NullPointerException ex){
            Log.e(TAG, "setDisplayHomeAsUpEnabled failed");
        }
        //mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        //mProgressBar.setVisibility(ProgressBar.VISIBLE);

        final SwipeRefreshLayout swipey = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipey.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh() {
                Log.i(TAG, "onRefresh called from SwipeRefreshLayout");
                updateScores();
            }
        });

        mlistView = (ListView)findViewById(R.id.scoreboard_list);
        mListAdapter = new ScoreboardListAdapter(this,R.layout.item_scorelist);
        mlistView.setAdapter(mListAdapter);

        refUsers = FirebaseDatabase.getInstance().getReference().child(Constants.USERS);
        DatabaseReference refPoints = FirebaseDatabase.getInstance().getReference().child("points/app");
        scoreQuery = refPoints.orderByValue().limitToLast(10);

        mValueEventListener = new ValueEventListener() //listens for query, gets <userid>:<score>
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                for (DataSnapshot scoreSnap : dataSnapshot.getChildren())
                {
                    String personId = scoreSnap.getKey();
                    final int score = scoreSnap.getValue(Integer.class);
                    Log.d(TAG, "onChildAdded " + personId + ": " + score);
                    refUsers.child(personId).addListenerForSingleValueEvent(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            ProfileBean beanie = dataSnapshot.getValue(ProfileBean.class);
                            beanie.setReserve(Integer.toString(score));
                            mListAdapter.insert(beanie,0);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "Well " + databaseError.getDetails());
                        }
                    });
                }
                swipey.setRefreshing(false);
                //mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Well " + databaseError.getDetails());
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateScores();
    }

    private void updateScores() {
        //mProgressBar.setVisibility(ProgressBar.VISIBLE);
        mListAdapter.clear();
        scoreQuery.addListenerForSingleValueEvent(mValueEventListener);
    }

    @Override
    protected void onPause() {
        mListAdapter.clear();
        super.onPause();
    }
}

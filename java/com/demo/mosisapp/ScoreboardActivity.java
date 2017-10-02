package com.demo.mosisapp;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
// Note: Known Issue: sometimes my score gets loaded last in list, even when i have more points. Solve: refresh
public class ScoreboardActivity extends AppCompatActivity
{
    private String TAG = "ScoreboardActivity";
    private ScoreboardListAdapter mListAdapter;

    private DatabaseReference refUsers;
    private Query scoreQuery;
    private ValueEventListener mValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final SwipeRefreshLayout swipey = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipey.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh() {
                Log.d(TAG, "onRefresh called from SwipeRefreshLayout");
                updateScores();
            }
        });

        ListView mlistView = (ListView) findViewById(R.id.scoreboard_list);
        mListAdapter = new ScoreboardListAdapter(this,R.layout.item_scorelist);
        mlistView.setAdapter(mListAdapter);

        refUsers = FirebaseDatabase.getInstance().getReference().child(Constants.USERS);
        DatabaseReference refPoints = FirebaseDatabase.getInstance().getReference().child("points/app");
        scoreQuery = refPoints.orderByValue().limitToLast(10);

        String me = FirebaseAuth.getInstance().getCurrentUser().getUid();
        refPoints.child(me).addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int myscore = dataSnapshot.getValue(Integer.class);
                    getSupportActionBar().setSubtitle("My Score: " + myscore);
                }
                else {
                    getSupportActionBar().setSubtitle("My Score: 0");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Well " + databaseError.getDetails());
            }
        });

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
        mListAdapter.clear();
        scoreQuery.addListenerForSingleValueEvent(mValueEventListener);
    }

    @Override
    protected void onPause() {
        mListAdapter.clear();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

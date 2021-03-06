package com.example.jeremiahvaris.justjournal;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements JournalEntryAdapter.EntryClickListener {
    // Intent Request codes
    private static final int RC_SIGN_IN = 1;
    private static final int NEW_JOURNAL_ENTRY_REQUEST_CODE = 2;

    RecyclerView mEntriesList;
    JournalEntryAdapter mAdapter;
    List<JournalEntry> mEntries;
    FloatingActionButton newEntryButton;
    Button signout;
    JournalEntriesViewModel mJournalEntriesViewModel;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mEntriesFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseUser mUser;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_JOURNAL_ENTRY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                JournalEntry entry =
                        new JournalEntry("Untitled",
                                data.getStringExtra(JournalEntryActivity.EXTRA_REPLY));
                Toast.makeText(this, entry.getContentSummary(), Toast.LENGTH_LONG).show();
                mJournalEntriesViewModel.insert(entry);
                mEntriesFirebaseDatabaseReference.push().setValue(entry);
            } else {
                Toast.makeText(getApplicationContext(), "Entry empty, not saved", Toast.LENGTH_LONG)
                        .show();
            }
        }
        // Firebase Login
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                mUser = FirebaseAuth.getInstance().getCurrentUser();
            } else finish();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setLogo(R.drawable.ic_quill_drawing_a_line);


        mEntriesList = (RecyclerView) findViewById(R.id.entries_rv);
        mEntriesList.setHasFixedSize(true);

        //Initialise Firebase Instanes
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        // Todo: Properly implement Firebase database
        mEntriesFirebaseDatabaseReference = mFirebaseDatabase.getReference().child("entries");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mEntriesList.setLayoutManager(layoutManager);

        mEntries = new ArrayList<>();

        // Todo: Add view for empty state
        mAdapter = new JournalEntryAdapter(this);
        mEntriesList.setAdapter(mAdapter);

        //Todo: credit authors
        /**Quill icon made by Freepik (https://www.flaticon.com/authors/freepik)
         *
         */

        mJournalEntriesViewModel = ViewModelProviders.of(this)
                .get(JournalEntriesViewModel.class);

        // Observe changes to the Journal Database and update the adapter accordingly
        mJournalEntriesViewModel.getAllEntries().observe(this, new Observer<List<JournalEntry>>() {
            @Override
            public void onChanged(@Nullable List<JournalEntry> journalEntries) {
                mEntries = mJournalEntriesViewModel.getAllEntries().getValue();
                mAdapter.setEntries(mEntries);
            }
        });


        newEntryButton = (FloatingActionButton) findViewById(R.id.new_entry_button);
        newEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.journal_entry_fragment_container,
                                new JournalEntryFragment())
                        .addToBackStack("Add entry")
                        .commit();

//                Intent newEntry = new Intent(MainActivity.this, JournalEntryActivity.class);
//                startActivityForResult(newEntry, NEW_JOURNAL_ENTRY_REQUEST_CODE);
            }
        });

        signout = (Button) findViewById(R.id.sign_out);
        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthUI.getInstance().signOut(MainActivity.this);
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Toast.makeText(MainActivity.this,
                            "Welcome " + user.getDisplayName(),
                            Toast.LENGTH_SHORT)
                            .show();
                    mUser = user;
                } else {
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onEntryClicked(int index, JournalEntry entry) {
        // Todo: Go to Edit Fragment
//        Toast.makeText(this, "Item " + (index + 1) + " clicked", Toast.LENGTH_SHORT).show();

        Bundle arguments = new Bundle();
        arguments.putSerializable(JournalEntryFragment.ENTRY, entry);
//        JournalEntry putEntry = (JournalEntry)arguments.getSerializable(JournalEntryFragment.ENTRY);
//        Toast.makeText(this, putEntry.getContentSummary(), Toast.LENGTH_SHORT).show();
        Fragment editFragment = new JournalEntryFragment();
        editFragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.journal_entry_fragment_container, editFragment)
                .addToBackStack("Edit entry")
                .commit();
    }


}

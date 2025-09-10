package com.example.librelog;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import io.github.billpottle.librelog.R;

import com.example.librelog.ui.ImportFragment;
import com.example.librelog.ui.SettingsFragment;
import com.example.librelog.ui.HomeFragment;
import com.example.librelog.ui.OutputFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_import) {
                selectedFragment = new ImportFragment();
            } else if (itemId == R.id.navigation_output) {
                selectedFragment = new OutputFragment();
            } else if (itemId == R.id.navigation_help) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        // Load the default fragment
        if (savedInstanceState == null) {
            // Ensure bottomNavigationView is not null before trying to set selected item
            if (bottomNavigationView != null) {
                 bottomNavigationView.setSelectedItemId(R.id.navigation_home);
            } else {
                // Fallback in case bottom_navigation is not found, though it should be.
                loadFragment(new HomeFragment());
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }
}

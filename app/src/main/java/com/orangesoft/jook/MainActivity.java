package com.orangesoft.jook;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends AppCompatActivity
{
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private FragmentManager.OnBackStackChangedListener onBackStackChangedListener = new
            FragmentManager.OnBackStackChangedListener()
            {
                @Override
                public void onBackStackChanged()
                {
                    syncActionBarArrowState();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        actionBar.setDisplayHomeAsUpEnabled(true);

        /**
         * Setup the DrawerLayout and NavigationView
         */
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        /**
         * Setup click events on the Navigation View items
         */
        setupNavigation(navigationView);

        setupTabs();

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name)
        {

            public void onDrawerClosed(View view)
            {
                syncActionBarArrowState();
            }

            public void onDrawerOpened(View drawerView)
            {
                drawerToggle.setDrawerIndicatorEnabled(true);
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
        getSupportFragmentManager().addOnBackStackChangedListener(onBackStackChangedListener);
        drawerToggle.syncState();
    }

    @Override
    protected void onDestroy()
    {
        getSupportFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);
        super.onDestroy();
    }

    private void syncActionBarArrowState()
    {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        drawerToggle.setDrawerIndicatorEnabled(backStackEntryCount == 0);
    }

    private void setupTabs()
    {
        TabFragmentPagerAdapter adapter = new TabFragmentPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupNavigation(NavigationView navigationView)
    {
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem)
            {
                drawerLayout.closeDrawers();

                if (menuItem.getItemId() == R.id.nav_main)
                {
                    setupTabs();
                }

                if (menuItem.getItemId() == R.id.nav_settings)
                {
                    startActivity(new Intent(getApplication(), SubsonicActivity.class));
                }

                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (drawerToggle.isDrawerIndicatorEnabled() && drawerToggle.onOptionsItemSelected(item))
        {
            // The action bar home/up action should open or close the drawer.
            switch (item.getItemId()) {
                case android.R.id.home:
                    drawerLayout.openDrawer(GravityCompat.START);
                    return true;
            }
        }
        else if (item.getItemId() == android.R.id.home && getSupportFragmentManager().popBackStackImmediate())
            return true;

        return super.onOptionsItemSelected(item);
    }

    // Make sure this is the method with just 'Bundle' as the signature
    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
    }

}

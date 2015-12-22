package uk.lancs.sharc.smat.controller;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.ogaclejapan.smarttablayout.SmartTabLayout;

import uk.lancs.sharc.smat.R;

public class Usermanual extends FragmentActivity {
    private ViewPager pager;
    private com.ogaclejapan.smarttablayout.SmartTabLayout indicator;
    private Button skip;
    private Button next;
    private Button prev;
    private final int totalScreen = 8;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usermanual);
        pager = (ViewPager)findViewById(R.id.pager);
        indicator = (SmartTabLayout)findViewById(R.id.indicator);
        skip = (Button)findViewById(R.id.skip);
        prev = (Button)findViewById(R.id.prev);
        next = (Button)findViewById(R.id.next);
        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pager.getCurrentItem() < totalScreen - 1) { // The last screen
                    pager.setCurrentItem(
                            pager.getCurrentItem() + 1,
                            true
                    );
                }
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pager.getCurrentItem() > 0) { // The last screen
                    pager.setCurrentItem(
                            pager.getCurrentItem() - 1,
                            true
                    );
                }
            }
        });

        indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == totalScreen - 1) {
                    next.setVisibility(View.GONE);
                } else {
                    next.setVisibility(View.VISIBLE);
                }

                if (position == 0) {
                    prev.setVisibility(View.GONE);
                } else {
                    prev.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void finishOnboarding() {
        // Get the shared preferences
        SharedPreferences preferences =
                getSharedPreferences("my_preferences", MODE_PRIVATE);

        // Set onboarding_complete to true
        preferences.edit()
                .putBoolean("onboarding_complete",true).apply();

        // Launch the main Activity, called MainActivity
        //Intent main = new Intent(this, MainActivity.class);
        //startActivity(main);

        // Close the OnboardingActivity
        finish();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_usermanual, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0 : return new UsermanualFragment1();
                case 1 : return new UsermanualFragment2();
                case 2 : return new UsermanualFragment3();
                case 3 : return new UsermanualFragment4();
                case 4 : return new UsermanualFragment5();
                case 5 : return new UsermanualFragment6();
                case 6 : return new UsermanualFragment7();
                case 7 : return new UsermanualFragment8();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return totalScreen;
        }
    };
}

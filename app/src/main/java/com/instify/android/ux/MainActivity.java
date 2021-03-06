package com.instify.android.ux;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instify.android.R;
import com.instify.android.app.AppController;
import com.instify.android.app.ForceRemoteConfigUpdateChecker;
import com.instify.android.helpers.SQLiteHandler;
import com.instify.android.listeners.OnSingleClickListener;
import com.instify.android.models.UserDataModel;
import com.instify.android.utils.GetScreenHeightWidthUtils;
import com.instify.android.ux.fragments.AttendanceFragment;
import com.instify.android.ux.fragments.ExperiencesFragment;
import com.instify.android.ux.fragments.FeedFragment;
import com.instify.android.ux.fragments.NotesFragment;
import com.instify.android.ux.fragments.TimeTableFragment;
import com.instify.android.ux.fragments.UnivNewsFragment;
import com.thefinestartist.finestwebview.FinestWebView;
import javax.annotation.Nullable;
import timber.log.Timber;

/**
 * Created by Abhish3k on 3/1/2016.
 */

public class MainActivity extends AppCompatActivity
    implements ForceRemoteConfigUpdateChecker.OnUpdateNeededListener {
  private static final String TAG = MainActivity.class.getSimpleName();

  private static final int ANIM_DURATION_TOOLBAR = 500;
  private static final int ANIM_DURATION_FAB = 500;

  private boolean pendingIntroAnimation = true;
  // Enable double press
  private boolean doubleBackToExitPressedOnce = false;
  /* Play Services Request required to check if Google Services is installed or not */
  private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
  public FloatingActionButton mSharedFab;
  public UserDataModel userInfoObject;
  // Declare Firebase User
  FirebaseUser mFirebaseUser;
  // Declare Database Reference
  DatabaseReference dbRef, userRef;
  ValueEventListener childListener;
  // Declare SQLite
  SQLiteHandler db = new SQLiteHandler(this);

  private DrawerLayout drawerLayout;
  private ViewPager mViewPager;
  // Declare AdView
  private AdView mAdView;
  View headerView;

  @Nullable @BindView(R.id.mToolbar) Toolbar toolbar;
  @Nullable @BindView(R.id.action_filter) MenuItem filterMenuItem;

  /**
   * The {@link PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;

  // [START add_lifecycle_methods]

  /**
   * Called when leaving the activity
   */
  @Override protected void onPause() {
    // Ad-view
    if (mAdView != null) {
      mAdView.pause();
    }
    // Remove Database Listener
    userRef.removeEventListener(childListener);
    super.onPause();
  }

  /**
   * Called when returning to the activity
   */
  @Override protected void onResume() {
    // Add Database Listener
    userRef.addValueEventListener(childListener);
    if (mAdView != null) {
      mAdView.resume();
    }
    super.onResume();
    // Ensures that user didn't un-install Google Play Services required for Firebase related tasks.
    if (!checkPlayServices()) {
      Toast.makeText(this,
          "Device doesn't have Google Play Services installed, some features may not work.",
          Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * Called before the activity is destroyed
   */
  @Override protected void onDestroy() {
    // Remove Database Listener
    userRef.removeEventListener(childListener);
    if (mAdView != null) {
      mAdView.removeAllViews();
      mAdView.destroy();
      finish();
    }
    super.onDestroy();
  }
  // [END add_lifecycle_methods]

  @Override public void onBackPressed() {
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else if (doubleBackToExitPressedOnce) {
      super.onBackPressed();
    } else {
      this.doubleBackToExitPressedOnce = true;
      Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
      new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    setupToolbar();

    if (savedInstanceState == null) {
      pendingIntroAnimation = false;
    }
    // Declare Views
    mSharedFab = findViewById(R.id.shared_fab);
    // Get the current logged-in user
    mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    // Initialize ForceUpdateChecker
    ForceRemoteConfigUpdateChecker.with(this).onUpdateNeeded(this).check();
    // Drawer Layout
    drawerLayout = findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
        R.string.content_description_open_navigation_drawer,
        R.string.content_description_close_navigation_drawer);
    drawerLayout.addDrawerListener(toggle);
    // [START] Initialize Navigation Drawer and Profile Picture
    NavigationView navView = findViewById(R.id.navigation_view);
    // Inflate header view
    headerView = navView.inflateHeaderView(R.layout.nav_header_main);
    // Setup Drawer Content
    setupDrawerContent(navView);
    drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
      @Override public void onDrawerSlide(View drawerView, float slideOffset) {
      }

      @Override public void onDrawerOpened(View drawerView) {
        navView.bringToFront();
      }

      @Override public void onDrawerClosed(View drawerView) {
      }

      @Override public void onDrawerStateChanged(int newState) {
      }
    });
    toggle.syncState();

    // toolbar.setNavigationIcon(R.drawable.ic_menu_white);

    /* [START] Setup Header View */
    ImageView navImageView = headerView.findViewById(R.id.nav_drawer_user_photo);
    TextView navTextViewName = headerView.findViewById(R.id.nav_drawer_header_text_name);
    TextView navTextViewEmail = headerView.findViewById(R.id.nav_drawer_header_text_email);

    // Download the image from ERP
    // new DownloadImage(navImageView).execute(db.getUserDetails().get("dept"));

    if (mFirebaseUser != null) {
      try {
        // Set profile picture from Firebase account
        Glide.with(this)
            .load(mFirebaseUser.getPhotoUrl().toString())
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
            .apply(new RequestOptions().placeholder(R.drawable.default_pic_face))
            .apply(new RequestOptions().centerCrop())
            .apply(new RequestOptions().priority(Priority.HIGH))
            .into(navImageView);
      } catch (Exception e) {
        Timber.d(e);
        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
      }
    }
    // Set the name
    navTextViewName.setText(db.getUserDetails().getName());
    navTextViewEmail.setText(db.getUserDetails().getEmail());

    // Click listeners
    navImageView.setOnClickListener(new OnSingleClickListener() {
      @Override public void onSingleClick(View view) {
        startActivity(new Intent(MainActivity.this, ProfilePictureFullScreenActivity.class));
      }
    });
    /* [END] Setup Header View **/

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    // Set the default tab as Campus Portal
    mViewPager.setCurrentItem(1);
    // Prevent fragments from destroying themselves
    mViewPager.setOffscreenPageLimit(3);

    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override public void onPageSelected(int position) {
        switch (position) {
          case 1:
            mSharedFab.show();
            break;
          default:
            mSharedFab.hide();
            break;
        }
      }

      @Override public void onPageScrollStateChanged(int state) {

      }
    });

    TabLayout mTabLayout = findViewById(R.id.tabLayout);
    mTabLayout.setupWithViewPager(mViewPager);

    // Check if Google Play Services is installed or not
    checkPlayServices();

    if (mFirebaseUser != null) {
      dbRef = FirebaseDatabase.getInstance().getReference();
      userRef = dbRef.child("users").child(mFirebaseUser.getUid());

      // User object generation for the database - for usage in all fragments
      childListener = userRef.addValueEventListener(new ValueEventListener() {
        @Override public void onDataChange(DataSnapshot dataSnapshot) {
          // Collecting users data to use through out the app
          userInfoObject = dataSnapshot.getValue(UserDataModel.class);
        }

        @Override public void onCancelled(DatabaseError databaseError) {

        }
      });
    } else {
      // User is signed out
      Timber.d(TAG, "onAuthStateChanged:signed_out");
      Intent homeIntent = new Intent(Intent.ACTION_MAIN);
      homeIntent.addCategory(Intent.CATEGORY_HOME);
      homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(homeIntent);
    }

    // Initialize Mobile Ads (AdWords)
    MobileAds.initialize(getApplicationContext(), getString(R.string.all_ad_app_id));

    // [START load_banner_ad]
    mAdView = findViewById(R.id.adView);
    AdRequest adRequestBanner = new AdRequest.Builder().build();
    // Load Ad
    mAdView.loadAd(adRequestBanner);
    // Add Ad Listener
    mAdView.setAdListener(new AdListener() {
      @Override public void onAdLoaded() {
        super.onAdLoaded();
        mAdView.animate().setStartDelay(400);
        mAdView.setVisibility(View.VISIBLE);
      }
    });
    // [END load_banner_ad]
  }

  protected void setupToolbar() {
    if (toolbar != null) {
      setSupportActionBar(toolbar);
    }
  }

  // Check if update to new version is needed or not
  @Override public void onUpdateNeeded(String updateUrl) {
    AlertDialog dialog = new AlertDialog.Builder(this).setTitle("New version available")
        .setMessage("Please update to new version to enjoy latest features and bug fixes!")
        .setPositiveButton("Update", new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            redirectStore(updateUrl);
          }
        })
        .setNegativeButton("No, thanks", new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        })
        .create();
    dialog.show();
  }

  private void redirectStore(String updateUrl) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  /**
   * Check the device to make sure it has the Google Play Services APK. If
   * it doesn't, display a dialog that allows users to download the APK from
   * the Google Play Store or enable it in the device's system settings.
   */
  private boolean checkPlayServices() {
    GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
    int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
      if (apiAvailability.isUserResolvableError(resultCode)) {
        apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
      } else {
        Timber.i(TAG, "This device is not supported.");
        finish();
      }
      return false;
    }
    return true;
  }

  private void setupDrawerContent(NavigationView navigationView) {
    navigationView.setNavigationItemSelectedListener(menuItem -> {
      menuItem.setChecked(true);
      switch (menuItem.getItemId()) {
        case R.id.nav_attendance:
          mViewPager.setCurrentItem(0);
          break;
        case R.id.nav_experiences:
          mViewPager.setCurrentItem(1);
          break;
        case R.id.nav_schedule:
          mViewPager.setCurrentItem(2);
          break;
        case R.id.nav_notes:
          mViewPager.setCurrentItem(3);
          break;
        case R.id.nav_srm_news:
          mViewPager.setCurrentItem(4);
          break;
        case R.id.nav_test_performance:
          startActivity(new Intent(MainActivity.this, TestPerformanceActivity.class));
          break;
        case R.id.nav_calculate_gpa:
          intentGPACalculator(MainActivity.this, "com.gupta.ishansh.gcmcalculator");
          break;
        case R.id.nav_feekart:
          FeekartWebView();
          break;
        case R.id.nav_feekart_history:
          startActivity(new Intent(MainActivity.this, FeePaymentHistoryActivity.class));
          break;
        case R.id.nav_send:
          String[] emails = { "abhishekuniyal09@gmail.com" };
          String subject = "I want to submit Feedback";
          String message = "Hi, ";
          Intent email = new Intent(Intent.ACTION_SENDTO);
          email.putExtra(Intent.EXTRA_EMAIL, emails);
          email.putExtra(Intent.EXTRA_SUBJECT, subject);
          email.putExtra(Intent.EXTRA_TEXT, message);
          email.setType("*/*");
          email.setData(Uri.parse("mailto:"));

          if (email.resolveActivity(getPackageManager()) != null) {
            startActivity(email);
          }
          break;
        case R.id.nav_share:
          redirectStore("market://details?id=com.instify.android");
          break;
        case R.id.nav_donate_us:
          startActivity(new Intent(MainActivity.this, SupportUsActivity.class));
          break;
        case R.id.nav_about:
          startActivity(new Intent(MainActivity.this, AboutActivity.class));
          break;
      }
      drawerLayout.closeDrawers();
      return true;
    });
  }

  @Nullable public Toolbar getToolbar() {
    return toolbar;
  }

  @Nullable public MenuItem getFilterMenuItem() {
    return filterMenuItem;
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    // Start Animations
    if (pendingIntroAnimation) {
      pendingIntroAnimation = false;
      startIntroAnimation();
    }
    return true;
  }

  private void startIntroAnimation() {
    mSharedFab.setTranslationY(2 * getResources().getDimensionPixelOffset(R.dimen.btn_fab_size));

    int actionbarSize = GetScreenHeightWidthUtils.dpToPx(56);
    getToolbar().setTranslationY(-actionbarSize);
    getToolbar().animate().translationY(0).setDuration(ANIM_DURATION_TOOLBAR).setStartDelay(300);
    // Start content Animation in fragment
    startContentAnimation();
  }

  private void startContentAnimation() {
    mSharedFab.animate()
        .translationY(0)
        .setInterpolator(new OvershootInterpolator(1.f))
        .setStartDelay(300)
        .setDuration(ANIM_DURATION_FAB)
        .start();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == android.R.id.home) {
      startActivity(new Intent(this, MainActivity.class));
      return true;
    } else if (id == R.id.action_search) {
      return true;
    } else if (id == R.id.action_filter) {
      return true;
    } else if (id == R.id.action_settings) {
      startActivity(new Intent(this, SettingsActivity.class));
    } else if (id == R.id.action_logout) {
      AppController.getInstance().logoutUser();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void intentGPACalculator(Context context, String packageName) {
    Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
    if (intent == null) {
      // Bring user to the market or let them choose an app?
      intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse("market://details?id=" + packageName));
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  private void FeekartWebView() {
    // Fetching the login details from the database
    SQLiteHandler db = new SQLiteHandler(this);
    String regNo = db.getUserDetails().getRegno();
    String pass = db.getUserDetails().getToken();

    // Notify the user about the app's features
    Toast.makeText(this, "Instify will automatically log you in!", Toast.LENGTH_LONG).show();

    new FinestWebView.Builder(this).theme(R.style.FinestWebViewTheme)
        .titleDefault("Feekart")
        .showUrl(false)
        .statusBarColorRes(R.color.colorPrimaryDark)
        .toolbarColorRes(R.color.colorPrimary)
        .titleColorRes(R.color.finestWhite)
        .urlColorRes(R.color.colorPrimaryLight)
        .iconDefaultColorRes(R.color.finestWhite)
        .progressBarColorRes(R.color.finestWhite)
        .stringResCopiedToClipboard(R.string.copied_to_clipboard)
        .stringResCopiedToClipboard(R.string.copied_to_clipboard)
        .stringResCopiedToClipboard(R.string.copied_to_clipboard)
        .showSwipeRefreshLayout(true)
        .updateTitleFromHtml(true)
        .swipeRefreshColorRes(R.color.colorPrimaryDark)
        .menuSelector(R.drawable.selector_light_theme)
        .menuTextGravity(Gravity.CENTER)
        .menuTextPaddingRightRes(R.dimen.defaultMenuTextPaddingLeft)
        .webViewDisplayZoomControls(true)
        .webViewDomStorageEnabled(true)
        .webViewJavaScriptEnabled(true)
        .webViewDisplayZoomControls(true)
        .webViewJavaScriptCanOpenWindowsAutomatically(true)
        .gradientDivider(false)
        .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit,
            R.anim.fragment_close_enter, R.anim.fragment_close_exit)
        .injectJavaScript("javascript:"
            + "document.getElementById('accountname').value = '"
            + regNo
            + "';"
            + "document.getElementById('password').value = '"
            + pass
            + "';"
            + "loginform()")
        .show("http://feekart.srmuniv.ac.in/srmopp/");
  }

  // Tab layout and Fragments

  /**
   * A placeholder fragment containing a simple view.
   */
  public static class PlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Default Constructor
     */
    public PlaceholderFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment newInstance(int sectionNumber) {
      PlaceholderFragment fragment = new PlaceholderFragment();
      Bundle args = new Bundle();
      args.putInt(ARG_SECTION_NUMBER, sectionNumber);
      fragment.setArguments(args);
      return fragment;
    }
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {
    // The total number of tabs
    static final int NUM_TABS = 5;
    static final int TAB_ATTENDANCE = 0;
    static final int TAB_EXPERIENCES = 1;
    static final int TAB_TIME_TABLE = 2;
    static final int TAB_NOTES = 3;
    static final int TAB_UNIVERSITY_NEWS = 4;
    private int[] imageResId = {
        R.drawable.ic_attendance_white, R.drawable.ic_whatshot_white,
        R.drawable.ic_time_table_white, R.drawable.ic_notes_white, R.drawable.ic_univ_news_white
    };

    private SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      switch (position) {
        case TAB_ATTENDANCE:
          return AttendanceFragment.newInstance();
        case TAB_EXPERIENCES:
          return ExperiencesFragment.newInstance();
        case TAB_TIME_TABLE:
          return TimeTableFragment.newInstance();
        case TAB_NOTES:
          return NotesFragment.newInstance();
        case TAB_UNIVERSITY_NEWS:
          return UnivNewsFragment.newInstance();
      }
      return PlaceholderFragment.newInstance(position);
    }

    @Override public int getCount() {
      return NUM_TABS;
    }

    @Override public CharSequence getPageTitle(int position) {

      Drawable image = ContextCompat.getDrawable(getApplication(), imageResId[position]);
      image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
      SpannableString sb = new SpannableString(" ");
      ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
      sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      return sb;
    }
  }
}
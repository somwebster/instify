package com.instify.android.ux;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.instify.android.R;
import com.instify.android.app.AppConfig;
import com.instify.android.app.AppController;
import com.instify.android.helpers.SQLiteHandler;
import com.instify.android.models.FeePaymentHistoryModel;
import com.instify.android.ux.adapters.FeePaymentHistoryAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by Abhish3k on 23-03-2017.
 */

public class FeePaymentHistoryActivity extends AppCompatActivity {
    private static final String TAG = FeePaymentHistoryActivity.class.getSimpleName();

    private SQLiteHandler db = new SQLiteHandler(this);
    private ProgressDialog mProgressDialog;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    // Declare AdView
    private AdView mAdView;

    String tag_string_req = "req_fee";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fee_details);

        if (getActionBar() != null) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Declare Views
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // [START load_banner_ad]
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        // [END load_banner_ad]

        // Setup Recycler View
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        // Fetch the details from the user
        getFeeDetails();

        // Get adapter and set it
        mAdapter = new FeePaymentHistoryAdapter(getDataSet());
        mRecyclerView.setAdapter(mAdapter);
    }

    private ArrayList<FeePaymentHistoryModel> getDataSet() {
        ArrayList results = new ArrayList<>();
        for (int index = 0; index < 0; index++) {
            FeePaymentHistoryModel obj = new FeePaymentHistoryModel("12-1-17 " + index,
                    "Secondary " + index, "Secondary " + index);
            results.add(index, obj);
        }

        return results;
    }

    public void getFeeDetails() {

        showProgressDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_FEE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Timber.d(TAG, "Login Response: " + response);
                hideProgressDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                        JSONArray user = jObj.getJSONArray("subjects");

                        for (int index = 0; index < user.length(); index++) {
                            //  String name = user.getString(index);
                            JSONObject subs = jObj.getJSONObject(user.getString(index));
                            FeePaymentHistoryModel obj = new FeePaymentHistoryModel(subs.getString("Date"),
                                    subs.getString("Narration"), subs.getString("Amount"));

                            ((FeePaymentHistoryAdapter) mAdapter).addItem(obj, index);
                        }

                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    // Disable the progress dialog
                    hideProgressDialog();
                    // Notify user
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Timber.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideProgressDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<>();

                String unm = db.getUserDetails().get("token");
                String pass = db.getUserDetails().get("created_at");
                params.put("regno", unm);
                params.put("pass", pass);

                return params;
            }

        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    // [START add_lifecycle_methods]

    /**
     * Called when leaving the activity
     */
    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    /**
     * Called when returning to the activity
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    /**
     * Called before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        // Destroy Volley's instance
        AppController.getInstance().cancelPendingRequests(tag_string_req);
        // Handle AdView
        if (mAdView != null) {
            mAdView.destroy();
            finish();
        }
        super.onDestroy();
    }
    // [END add_lifecycle_methods]

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.Loading));
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}

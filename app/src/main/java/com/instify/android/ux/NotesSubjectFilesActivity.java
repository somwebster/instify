package com.instify.android.ux;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.instify.android.R;
import com.instify.android.models.NotesFileModel;
import com.instify.android.ux.adapters.NotesFileAdapter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class NotesSubjectFilesActivity extends AppCompatActivity {

  // PERMS
  private static final int RC_CAMERA_PERMISSION = 101;
  private static final int RC_STORAGE_PERMISSION = 123;
  private static final int DOC_SELECT = 1;
  private static final int PDF_SELECT = 2;
  private static final int IMAGE_SELECT = 3;
  private static final int VIDEO_SELECT = 4;
  private static final int AUDIO_SELECT = 5;
  private static final int OTHER_SELECT = 6;

  RecyclerView mNotesView;
  SearchView searchView = null;
  @BindView(R.id.error_message) TextView errorMessage;
  @BindView(R.id.placeholder_error) LinearLayout placeholderError;
  private Uri mFileUri = null;
  private Uri mFilePath;
  private String type;
  FirebaseRecyclerAdapter<NotesFileModel, NotesFileAdapter.MyHolder> adapter;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_notes_subject_files);
    ButterKnife.bind(this);
    setTitle(Html.fromHtml("<small>" + getIntent().getStringExtra("code") + "</small>"));

    mNotesView = findViewById(R.id.recycler_view_notes);
    setNotesFirebase(getIntent().getStringExtra("code"));
    // getNotes(getIntent().getStringExtra("code"));
  }

  private void setNotesFirebase(String subjectCode) {

    Query query = FirebaseDatabase.getInstance().getReference().child("notes").child(subjectCode);
    FirebaseRecyclerOptions<NotesFileModel> options =
        new FirebaseRecyclerOptions.Builder<NotesFileModel>().setQuery(query, NotesFileModel.class)
            .build();
    adapter = new FirebaseRecyclerAdapter<NotesFileModel, NotesFileAdapter.MyHolder>(options) {
      @Override
      public NotesFileAdapter.MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new instance of the ViewHolder, in this case we are using a custom
        // layout called R.layout.message for each item
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.card_view_notes_subjects_item, parent, false);

        return new NotesFileAdapter.MyHolder(view);
      }

      @Override
      protected void onBindViewHolder(@NonNull NotesFileAdapter.MyHolder viewHolder, int position,
          @NonNull NotesFileModel model) {
        viewHolder.setDataToView(model);
        viewHolder.cv.setOnClickListener(v -> {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(model.getNotefile())));
        });
      }

      @Override public int getItemCount() {
        if (super.getItemCount() == 0) {
          showErrorPlaceholder("No notes to display, Be the first to upload!");
        } else {
          hidePlaceHolder();
        }
        return super.getItemCount();
      }
    };

    mNotesView.setLayoutManager(new LinearLayoutManager(NotesSubjectFilesActivity.this));

    mNotesView.setAdapter(adapter);
  }

  @Override public void onStart() {
    if (adapter != null) adapter.startListening();
    super.onStart();
  }

  @Override public void onStop() {
    if (adapter != null) adapter.stopListening();
    super.onStop();
  }

  @Override public void onPause() {
    if (adapter != null) adapter.stopListening();
    super.onPause();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // adds item to action bar
    getMenuInflater().inflate(R.menu.upload_menu_main, menu);
    // Get Search item from action bar and Get Search service
    MenuItem searchItem = menu.findItem(R.id.action_search);
    SearchManager searchManager =
        (SearchManager) NotesSubjectFilesActivity.this.getSystemService(Context.SEARCH_SERVICE);
    if (searchItem != null) {
      searchView = (SearchView) searchItem.getActionView();
    }
    if (searchView != null) {
      searchView.setSearchableInfo(
          searchManager.getSearchableInfo(NotesSubjectFilesActivity.this.getComponentName()));
      searchView.setIconified(true);
    }

    MenuItem enai = menu.findItem(R.id.action_add_note);

    enai.setOnMenuItemClickListener(item -> {
      BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
      View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_upload_notes, null);
      bottomSheetDialog.setContentView(sheetView);
      bottomSheetDialog.show();
      LinearLayout doc = sheetView.findViewById(R.id.doc);
      LinearLayout pdf = sheetView.findViewById(R.id.pdf);
      LinearLayout audio = sheetView.findViewById(R.id.audio);
      LinearLayout image = sheetView.findViewById(R.id.image);
      LinearLayout video = sheetView.findViewById(R.id.video);
      LinearLayout camera = sheetView.findViewById(R.id.camera);
      LinearLayout other = sheetView.findViewById(R.id.other);
      doc.setOnClickListener(v -> {
        type = "doc";
        requestStoragePermission();
      });
      pdf.setOnClickListener(v -> {
        type = "pdf";
        requestStoragePermission();
      });
      camera.setOnClickListener(v -> {
        requestCameraPermission();
      });
      video.setOnClickListener(v -> {
        type = "video";
        requestStoragePermission();
      });
      audio.setOnClickListener(v -> {
        type = "audio";
        requestStoragePermission();
      });
      image.setOnClickListener(v -> {
        type = "image";
        requestStoragePermission();
      });
      other.setOnClickListener(v -> {
        type = "other";
        requestStoragePermission();
      });

      return true;
    });
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    return super.onOptionsItemSelected(item);
  }

  // Requesting permission
  @AfterPermissionGranted(RC_CAMERA_PERMISSION) private void requestCameraPermission() {
    String[] perms = { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    if (EasyPermissions.hasPermissions(this, perms)) {
      // Choose file storage location
      dispatchTakePictureIntent();
    } else {
      // Ask for one permission
      EasyPermissions.requestPermissions(this, getString(R.string.rationale_camera),
          RC_CAMERA_PERMISSION, perms);
    }
  }

  // Requesting permission
  @AfterPermissionGranted(RC_STORAGE_PERMISSION) private void requestStoragePermission() {
    if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
      // Have permission, do the thing!
      switch (type) {
        case "pdf": {
          Intent intent = new Intent();
          intent.setType("application/pdf");
          intent.setAction(Intent.ACTION_GET_CONTENT);
          startActivityForResult(Intent.createChooser(intent, "Complete action using... "),
              PDF_SELECT);
          break;
        }
        case "doc": {
          Intent intent = new Intent();
          intent.setType("application/docx");
          intent.setAction(Intent.ACTION_GET_CONTENT);
          startActivityForResult(Intent.createChooser(intent, "Complete action using... "),
              DOC_SELECT);
          break;
        }
        case "audio": {
          Intent intent = new Intent();
          intent.setType("audio/*");
          intent.setAction(Intent.ACTION_GET_CONTENT);
          startActivityForResult(Intent.createChooser(intent, "Complete action using... "),
              AUDIO_SELECT);
          break;
        }
        case "image": {
          Intent intent = new Intent();
          intent.setType("image/*");
          intent.setAction(Intent.ACTION_GET_CONTENT);
          startActivityForResult(Intent.createChooser(intent, "Complete action using... "),
              IMAGE_SELECT);
          break;
        }
        case "video": {
          Intent intent = new Intent();
          intent.setType("video/*");
          intent.setAction(Intent.ACTION_GET_CONTENT);
          startActivityForResult(Intent.createChooser(intent, "Complete action using... "),
              VIDEO_SELECT);
          break;
        }
        default: {
          Intent intent = new Intent();
          intent.setType("*/*");
          intent.setAction(Intent.ACTION_GET_CONTENT);
          startActivityForResult(Intent.createChooser(intent, "Complete action using... "),
              OTHER_SELECT);
          break;
        }
      }
    } else {
      // Ask for one permission
      EasyPermissions.requestPermissions(this, getString(R.string.rationale_camera),
          RC_STORAGE_PERMISSION, Manifest.permission.READ_EXTERNAL_STORAGE);
    }
  }

  // [START] EasyPermissions Default Functions
  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    // EasyPermissions handles the request result.
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }
  // [END] EasyPermission Default Functions

  // Handling the image chooser activity result
  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RC_CAMERA_PERMISSION && resultCode == RESULT_OK && mFileUri != null) {
      mFilePath = data.getData();
      Intent intent = new Intent(getApplicationContext(), UploadNotesActivity.class);
      intent.putExtra("code", getIntent().getStringExtra("code"));
      intent.putExtra("fileuri", mFileUri.toString());
      intent.putExtra("filetype", "image");
      startActivity(intent);
    } else if (requestCode == RC_STORAGE_PERMISSION && resultCode == RESULT_OK) {
      mFilePath = data.getData();
      Intent intent = new Intent(getApplicationContext(), UploadNotesActivity.class);
      intent.putExtra("code", getIntent().getStringExtra("code"));
      intent.putExtra("fileuri", mFilePath.toString());
      intent.putExtra("filetype", "image");
      startActivity(intent);
    } else if (requestCode == PDF_SELECT && resultCode == RESULT_OK) {
      mFilePath = data.getData();
      Intent intent = new Intent(getApplicationContext(), UploadNotesActivity.class);
      intent.putExtra("code", getIntent().getStringExtra("code"));
      intent.putExtra("fileuri", mFilePath.toString());
      intent.putExtra("filetype", "pdf");
      startActivity(intent);
    } else if (requestCode == DOC_SELECT && resultCode == RESULT_OK) {
      mFilePath = data.getData();
      Intent intent = new Intent(getApplicationContext(), UploadNotesActivity.class);
      intent.putExtra("code", getIntent().getStringExtra("code"));
      intent.putExtra("fileuri", mFilePath.toString());
      intent.putExtra("filetype", "doc");
      startActivity(intent);
    } else if (requestCode == IMAGE_SELECT && resultCode == RESULT_OK) {
      mFilePath = data.getData();
      Intent intent = new Intent(getApplicationContext(), UploadNotesActivity.class);
      intent.putExtra("code", getIntent().getStringExtra("code"));
      intent.putExtra("fileuri", mFilePath.toString());
      intent.putExtra("filetype", "image");
      startActivity(intent);
    } else if (requestCode == VIDEO_SELECT && resultCode == RESULT_OK) {
      mFilePath = data.getData();
      Intent intent = new Intent(getApplicationContext(), UploadNotesActivity.class);
      intent.putExtra("code", getIntent().getStringExtra("code"));
      intent.putExtra("fileuri", mFilePath.toString());
      intent.putExtra("filetype", "video");
      startActivity(intent);
    } else if (requestCode == AUDIO_SELECT && resultCode == RESULT_OK) {
      mFilePath = data.getData();
      Intent intent = new Intent(getApplicationContext(), UploadNotesActivity.class);
      intent.putExtra("code", getIntent().getStringExtra("code"));
      intent.putExtra("fileuri", mFilePath.toString());
      intent.putExtra("filetype", "audio");
      startActivity(intent);
    } else if (requestCode == OTHER_SELECT && resultCode == RESULT_OK) {
      mFilePath = data.getData();
      Intent intent = new Intent(getApplicationContext(), UploadNotesActivity.class);
      intent.putExtra("code", getIntent().getStringExtra("code"));
      intent.putExtra("fileuri", mFilePath.toString());
      intent.putExtra("filetype", "other");
      startActivity(intent);
    } else if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
      // Do something after user returned from app settings screen, like showing a Toast.
      Toast.makeText(this, R.string.returned_from_app_settings_to_activity, Toast.LENGTH_SHORT)
          .show();
    }
  }

  public void showErrorPlaceholder(String message) {
    if (placeholderError != null && errorMessage != null) {
      if (placeholderError.getVisibility() != View.VISIBLE) {
        placeholderError.setVisibility(View.VISIBLE);
      }
      errorMessage.setText(message);
    }
  }

  public void hidePlaceHolder() {
    if (placeholderError != null && errorMessage != null) {
      if (placeholderError.getVisibility() == View.VISIBLE) {
        placeholderError.setVisibility(View.INVISIBLE);
      }
      errorMessage.setText("Something went wrong. Try again!");
    }
  }

  String mCurrentPhotoPath;

  private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(imageFileName,  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */);

    // Save a file: path for use with ACTION_VIEW intents
    mCurrentPhotoPath = image.getAbsolutePath();
    return image;
  }

  private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        photoFile = createImageFile();
      } catch (IOException ex) {
        // Error occurred while creating the File
      }
      // Continue only if the File was successfully created
      if (photoFile != null) {
        mFileUri = FileProvider.getUriForFile(this, "com.instify.android.provider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
        startActivityForResult(takePictureIntent, RC_CAMERA_PERMISSION);
      }
    }
  }
}

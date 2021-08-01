/*
    This file is part of the dandelion*.

    dandelion* is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    dandelion* is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the dandelion*.

    If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.dfa.diaspora_android.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.github.dfa.diaspora_android.App;
import com.github.dfa.diaspora_android.BuildConfig;
import com.github.dfa.diaspora_android.R;
import com.github.dfa.diaspora_android.data.DiasporaUserProfile;
import com.github.dfa.diaspora_android.ui.theme.ThemedAlertDialogBuilder;
import com.github.dfa.diaspora_android.util.AppLog;
import com.github.dfa.diaspora_android.util.AppSettings;
import com.github.dfa.diaspora_android.util.ContextUtils;
import com.github.dfa.diaspora_android.util.DiasporaUrlHelper;
import com.github.dfa.diaspora_android.web.BrowserFragment;
import com.github.dfa.diaspora_android.web.DiasporaStreamWebChromeClient;
import com.github.dfa.diaspora_android.web.FileUploadWebChromeClient;
import com.github.dfa.diaspora_android.web.WebHelper;

import net.gsantner.opoc.util.PermissionChecker;
import net.gsantner.opoc.util.ShareUtil;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Fragment that displays the Stream of the diaspora* user
 * Created by vanitas on 26.09.16.
 */

public class DiasporaStreamFragment extends BrowserFragment {
    public static final String TAG = "com.github.dfa.diaspora_android.StreamFragment";

    protected DiasporaUrlHelper urls;

    private ValueCallback<Uri[]> imageUploadFilePathCallbackNew;
    private ValueCallback<Uri> imageUploadFilePathCallbackOld;
    private String mCameraPhotoPath;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.urls = new DiasporaUrlHelper(appSettings);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.setWebChromeClient(new DiasporaStreamWebChromeClient(webView, progressBar, fileUploadCallback, sharedTextCallback));
                webView.getSettings().setJavaScriptEnabled(true);
                webView.addJavascriptInterface(new JavaScriptInterface(), "AndroidBridge");
                if (((MainActivity) getActivity()).getTextToBeShared() != null) {
                    loadUrl(urls.getNewPostUrl());
                } else if (webView.getUrl() == null) {
                    loadUrl(urls.getStreamUrl());
                }
            }
        });

    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.stream__menu_top, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            menu.findItem(R.id.action_share_pdf).setVisible(true);
        }

        final boolean darkBg = ContextUtils.get().shouldColorOnTopBeLight(AppSettings.get().getPrimaryColor());
        ContextUtils.get().tintMenuItems(menu, true, ContextCompat.getColor(getActivity(), darkBg ? R.color.white : R.color.black));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        AppLog.d(this, "onActivityResult(): " + requestCode);
        switch (requestCode) {
            case MainActivity.INPUT_FILE_REQUEST_CODE_NEW:
            case MainActivity.INPUT_FILE_REQUEST_CODE_OLD:
                AppLog.v(this, "INPUT_FILE_REQUEST_CODE: " + requestCode);
                onImageUploadResult(requestCode, resultCode, data);
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AppLog.d(this, "StreamFragment.onOptionsItemSelected()");
        ShareUtil shu = new ShareUtil(getContext());
        PermissionChecker permc = new PermissionChecker(getActivity());
        switch (item.getItemId()) {
            case R.id.action_reload: {
                if (WebHelper.isOnline(getContext())) {
                    reloadUrl();
                    return true;
                } else {
                    return false;
                }
            }

            case R.id.action_go_to_top: {
                ObjectAnimator anim = ObjectAnimator.ofInt(webView, "scrollY", webView.getScrollY(), 0);
                anim.setDuration(400);
                anim.start();
                return true;
            }

            case R.id.action_share_link: {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, webView.getTitle());
                sharingIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_dotdotdot)));
                return true;
            }

            case R.id.action_share_pdf: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    shu.createPdf(webView, "dandelion-" + ShareUtil.SDF_SHORT.format(new Date()));
                }
                return true;
            }

            case R.id.action_share_link_to_clipboard: {
                shu.setClipboard(webView.getUrl());
                Toast.makeText(getContext(), R.string.link_adress_copied, Toast.LENGTH_SHORT).show();
                return true;
            }

            case R.id.action_create_launcher_shortcut: {
                if (webView.getUrl() != null) {
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(webView.getUrl()));
                    shu.createLauncherDesktopShortcut(intent, R.drawable.ic_launcher, webView.getTitle());
                }
                return true;
            }

            case R.id.action_take_screenshot: {
                if (permc.doIfExtStoragePermissionGranted(getString(R.string.screenshot_permission__appspecific))) {
                    File fileSaveDirectory = appSettings.getAppSaveDirectory();
                    if (permc.mkdirIfStoragePermissionGranted(fileSaveDirectory)) {
                        Bitmap bmp = ShareUtil.getBitmapFromWebView(webView);
                        String filename = "dandelion-" + ShareUtil.SDF_SHORT.format(new Date()) + ".jpg";
                        _cu.writeImageToFile(new File(fileSaveDirectory, filename), bmp);
                        Snackbar.make(webView, getString(R.string.saving_screenshot_as)
                                + " " + filename, Snackbar.LENGTH_LONG).show();
                    }
                }
                return true;
            }

            case R.id.action_share_screenshot: {
                if (permc.doIfExtStoragePermissionGranted(getString(R.string.screenshot_permission__appspecific))) {
                    shu.shareImage(ShareUtil.getBitmapFromWebView(webView));
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void onImageUploadResult(int requestCode, int resultCode, Intent data) {
        AppLog.d(this, "onImageUploadResult");
        switch (requestCode) {
            case MainActivity.INPUT_FILE_REQUEST_CODE_NEW: {
                AppLog.v(this, "Upload image using recent method (Lollipop+)");
                if (imageUploadFilePathCallbackNew == null || resultCode != Activity.RESULT_OK) {
                    AppLog.e(this, "Callback is null: " + (imageUploadFilePathCallbackNew == null)
                            + " resultCode: " + resultCode);
                    if (imageUploadFilePathCallbackNew != null)
                        imageUploadFilePathCallbackNew.onReceiveValue(new Uri[]{});
                    return;
                }
                Uri[] results = null;
                if (data == null) {
                    if (mCameraPhotoPath != null) {
                        AppLog.v(this, "Intent data is null. Try to parse cameraPhotoPath");
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    } else {
                        AppLog.w(this, "Intent data is null and cameraPhotoPath is null");
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        AppLog.v(this, "Intent has data. Try to parse dataString");
                        results = new Uri[]{Uri.parse(dataString)};
                    } else {
                        AppLog.w(this, "dataString is null");
                    }
                }
                AppLog.v(this, "handle received result over to callback");
                imageUploadFilePathCallbackNew.onReceiveValue(results);
                imageUploadFilePathCallbackNew = null;
                return;
            }
            case MainActivity.INPUT_FILE_REQUEST_CODE_OLD: {
                AppLog.v(this, "Upload image using legacy method (Jelly Bean, Kitkat)");
                if (imageUploadFilePathCallbackOld == null || resultCode != Activity.RESULT_OK) {
                    AppLog.e(this, "Callback is null: " + (imageUploadFilePathCallbackOld == null)
                            + " resultCode: " + resultCode);
                    if (imageUploadFilePathCallbackOld != null)
                        imageUploadFilePathCallbackOld.onReceiveValue(null);
                    return;
                }
                Uri results = null;
                if (data == null) {
                    if (mCameraPhotoPath != null) {
                        AppLog.v(this, "Intent has no data. Try to parse cameraPhotoPath");
                        results = Uri.parse(mCameraPhotoPath);
                    } else {
                        AppLog.w(this, "Intent has no data and cameraPhotoPath is null");
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        AppLog.v(this, "Intent has data. Try to parse dataString");
                        results = Uri.parse(dataString);
                    } else {
                        AppLog.w(this, "dataString is null");
                    }
                }
                AppLog.v(this, "handle received result over to callback");
                imageUploadFilePathCallbackOld.onReceiveValue(results);
                imageUploadFilePathCallbackOld = null;
            }
        }
    }

    protected DiasporaStreamWebChromeClient.SharedTextCallback sharedTextCallback = new DiasporaStreamWebChromeClient.SharedTextCallback() {
        @Override
        public String getSharedText() {
            if (getActivity() != null) {
                return ((MainActivity) getActivity()).getTextToBeShared();
            }
            return "";
        }

        @Override
        public void setSharedText(String shared) {
            ((MainActivity) getActivity()).setTextToBeShared(shared);
        }
    };

    protected FileUploadWebChromeClient.FileUploadCallback fileUploadCallback = new FileUploadWebChromeClient.FileUploadCallback() {
        @Override
        public boolean imageUpload(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            if (Build.VERSION.SDK_INT >= 23) {
                int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        new ThemedAlertDialogBuilder(getContext(), appSettings)
                                .setMessage(R.string.image_permission_description__appspecific)
                                .setNegativeButton(android.R.string.no, null)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (android.os.Build.VERSION.SDK_INT >= 23)
                                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    MainActivity.REQUEST_CODE_ASK_PERMISSIONS);
                                    }
                                })
                                .show();
                        return false;
                    }
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MainActivity.REQUEST_CODE_ASK_PERMISSIONS);
                    return false;
                }
            }
            AppLog.v(this, "onOpenFileChooser");
            imageUploadFilePathCallbackNew = filePathCallback;
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile;
                try {
                    photoFile = ContextUtils.get().createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    AppLog.e(this, "ERROR creating temp file: " + ex.toString());
                    // Error occurred while creating the File
                    Snackbar.make(webView, R.string.unable_to_load_image, Snackbar.LENGTH_LONG).show();
                    return false;
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");
            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            AppLog.d(this, "startActivityForResult");
            startActivityForResult(chooserIntent, MainActivity.INPUT_FILE_REQUEST_CODE_NEW);
            return true;
        }

        @Override
        public void legacyImageUpload(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            AppLog.v(this, "openFileChooser(ValCallback<Uri>, String, String");
            imageUploadFilePathCallbackOld = uploadMsg;
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.putExtra("return-data", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            AppLog.v(this, "startActivityForResult");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), MainActivity.INPUT_FILE_REQUEST_CODE_OLD);
        }
    };

    private class JavaScriptInterface {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void setUserProfile(final String webMessage) throws JSONException {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        App app = ((App) activity.getApplication());
                        final DiasporaUserProfile pup = app.getDiasporaUserProfile();
                        if (pup.isRefreshNeeded()) {
                            try {
                                // Try to very fail-safe check if user information gets really loaded from correct pod
                                if (!webView.getUrl().startsWith(app.getSettings().getPod().getPodUrl().getBaseUrl())) {
                                    return;
                                }
                            } catch (Exception ignored) {
                                return;
                            }
                            AppLog.v(this, "DiasporaUserProfile needs refresh; Try to parse JSON");
                            pup.parseJson(webMessage);
                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    pup.analyzeUrl(webView.getUrl());
                                }
                            });
                        }
                    }
                });
            }
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void contentHasBeenShared() {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).setTextToBeShared(null);
            }
        }
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }
}

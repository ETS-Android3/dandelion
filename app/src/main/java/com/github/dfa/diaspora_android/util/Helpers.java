/*
    This file is part of the Diaspora for Android.

    Diaspora for Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Diaspora for Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Diaspora for Android.

    If not, see <http://www.gnu.org/licenses/>.
 */
 
package com.github.dfa.diaspora_android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.webkit.WebView;

import com.github.dfa.diaspora_android.App;
import com.github.dfa.diaspora_android.R;
import com.github.dfa.diaspora_android.data.AppSettings;
import com.github.dfa.diaspora_android.data.PodAspect;
import com.github.dfa.diaspora_android.data.PodUserProfile;

import java.util.Locale;

public class Helpers {

    public static boolean isOnline(Context context) {
        ConnectivityManager cnm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cnm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    public static void animateToActivity(Activity from, Class to, boolean finishFromActivity) {
        Intent intent = new Intent(from, to);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        if (finishFromActivity) {
            from.finish();
        }
    }

    public static void applyDiasporaMobileSiteChanges(final WebView wv) {
        wv.loadUrl("javascript: ( function() {" +
                "    document.documentElement.style.paddingBottom = '260px';" +
                "    document.getElementById('main').style.paddingTop = '5px';" +
                "    if(document.getElementById('main_nav')) {" +
                "        document.getElementById('main_nav').parentNode.removeChild(" +
                "        document.getElementById('main_nav'));" +
                "    } else if (document.getElementById('main-nav')) {" +
                "        document.getElementById('main-nav').parentNode.removeChild(" +
                "        document.getElementById('main-nav'));" +
                "    }" +
                "})();");
    }

    public static void getNotificationCount(final WebView wv) {
        wv.loadUrl("javascript: ( function() {" +
                "if (document.getElementById('notification')) {" +
                "       var count = document.getElementById('notification').innerHTML;" +
                "       AndroidBridge.setNotificationCount(count.replace(/(\\r\\n|\\n|\\r)/gm, \"\"));" +
                "    } else {" +
                "       AndroidBridge.setNotificationCount('0');" +
                "    }" +
                "    if (document.getElementById('conversation')) {" +
                "       var count = document.getElementById('conversation').innerHTML;" +
                "       AndroidBridge.setConversationCount(count.replace(/(\\r\\n|\\n|\\r)/gm, \"\"));" +
                "    } else {" +
                "       AndroidBridge.setConversationCount('0');" +
                "    }" +
                "})();");
    }

    public static void getUserProfile(final WebView wv) {
        // aspects":[{"id":124934,"name":"Friends","selected":true},{"id":124937,"name":"Liked me","selected":false},{"id":124938,"name":"Follow","selected":false},{"id":128327,"name":"Nur ich","selected":false}]
        wv.loadUrl("javascript: ( function() {" +
                "    if (typeof gon !== 'undefined' && typeof gon.user !== 'undefined') {" +
                "        var followed_tags = document.getElementById(\"followed_tags\");" +
                "        if(followed_tags != null) {" +
                "            try {" +
                "                var links = followed_tags.nextElementSibling.children[0].children;" +
                "                var tags = [];" +
                "                for(var i = 0; i < links.length - 1; i++) {" + // the last element is "Manage followed tags" link
                "                    tags.push(links[i].innerText.substring(1));" +
                "                }" +
                "                gon.user[\"android_app.followed_tags\"] = tags;" +
                "            } catch(e) {}" +
                "        }" +
                "       var userProfile = JSON.stringify(gon.user);" +
                "       AndroidBridge.setUserProfile(userProfile.toString());" +
                "    } " +
                "})();");
    }

    public static void showAspectList(final WebView wv, final App app) {
        wv.stopLoading();
        PodUserProfile profile = app.getPodUserProfile();
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body style='margin-top: 25px; margin-left:auto;margin-right:auto; font-size: 400%;'>");

        // Content
        for (PodAspect aspect : profile.getAspects()) {
            sb.append("<span style='margin-left: 30px; '></span>&raquo; &nbsp;");
            sb.append(aspect.toHtmlLink(app));
            sb.append("<hr style='height:5px;' />");
        }

        // End
        sb.append("</body></html>");
        wv.loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF-16", null);
    }

    public static void showFollowedTagsList(final WebView wv, final App app) {
        wv.stopLoading();
        PodUserProfile profile = app.getPodUserProfile();
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body style='margin-top: 25px; margin-left:auto;margin-right:auto; font-size: 400%;'>");

        // Content
        AppSettings appSettings = app.getSettings();
        sb.append("<span style='margin-left: 30px; '></span>&raquo; &nbsp;");
        sb.append(String.format(Locale.getDefault(),
                "<a href='https://%s/followed_tags' style='color: #000000; text-decoration: none;'><b>%s</b></a>",
                appSettings.getPodDomain(), app.getString(R.string.all_tags)));
        sb.append("<hr style='height:5px;' />");
        for (String tag: profile.getFollowedTags()) {
            sb.append("<span style='margin-left: 30px; '></span>&raquo; &nbsp;");
            sb.append(String.format(Locale.getDefault(),
                    "<a href='https://%s/tags/%s' style='color: #000000; text-decoration: none;'>#%s</a>",
                    appSettings.getPodDomain(), tag, tag));
            sb.append("<hr style='height:5px;' />");
        }

        // End
        sb.append("</body></html>");
        wv.loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF-16", null);
    }

}

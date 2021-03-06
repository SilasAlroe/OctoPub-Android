package dk.alroe.apps.octopub;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.IOException;

import dk.alroe.apps.octopub.model.Thread;
import dk.alroe.apps.octopub.model.UserId;

/**
 * Created by silasa on 1/4/17.
 */

public abstract class BaseActivity extends AppCompatActivity {
    private static final int DARK = 1;
    private static final int BRIGHT = 2;
    CollapsingToolbarLayout collapsingToolbar;
    private PendingIntent pendingIntent;
    Toolbar toolbar;
    private Menu menu;

    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        if (ColorHelper.isBrightColor(Color.parseColor("#" + getID().getId()))) {
            setUIColor(BRIGHT);
        } else {
            setUIColor(DARK);
        }

        menu.findItem(R.id.view_id).setTitle(getID().getId());
        menu.findItem(R.id.action_message_alarm).setChecked(getIsMessageAlarmEnabled());
        //Initialize alarm stuff
        //TODO move this to a more appropriate place.
        Intent alarmIntent = new Intent(BaseActivity.this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(BaseActivity.this, 0, alarmIntent, 0);

        return true;
    }
    private boolean getIsMessageAlarmEnabled(){
        SharedPreferences userData = getSharedPreferences("userData", 0);
        return userData.getBoolean("messageAlarm", false);
    }

    private void setUIColor(int color) {
        MenuInflater inflater = getMenuInflater();
        if (color == BRIGHT) {
            getSupportActionBar().getThemedContext().setTheme(R.style.AppBarLight);
            inflater.inflate(R.menu.default_menu_black, menu);
            if (collapsingToolbar != null) {
                collapsingToolbar.setExpandedTitleColor(Color.BLACK);
                collapsingToolbar.setCollapsedTitleTextColor(Color.BLACK);
                collapsingToolbar.setExpandedTitleTextAppearance(R.style.AppBarDark);
            } else {
                toolbar.setTitleTextColor(Color.BLACK);
            }
            //toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_more_vert_black_24dp));
            //setTheme(R.style.AppTheme);
        } else {
            getSupportActionBar().getThemedContext().setTheme(R.style.AppBarDark);
            inflater.inflate(R.menu.default_menu_white, menu);
            if (collapsingToolbar != null) {
                collapsingToolbar.setExpandedTitleColor(Color.WHITE);
                collapsingToolbar.setCollapsedTitleTextColor(Color.WHITE);
            } else {
                toolbar.setTitleTextColor(Color.WHITE);
            }
            //toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_more_vert_white_24dp));
            //setTheme(R.style.AppThemeDark);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences.Editor editor = getSharedPreferences("userData",0).edit();
        editor.putBoolean("isOpen",true);
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = getSharedPreferences("userData",0).edit();
        editor.putBoolean("isOpen",false);
        editor.putBoolean("wasLastOpen",true);
        editor.apply();
    }

    private void goToHelp() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    private void goToThreadAdd() {
        Intent intent = new Intent(this, ThreadAddActivity.class);
        startActivity(intent);
    }

    void goToThread(Thread thread) {
        Intent intent = new Intent(this, ThreadActivity.class);
        intent.putExtra("ThreadID", thread.getId());
        intent.putExtra("ThreadTitle", thread.getTitle());
        startActivity(intent);
    }

    UserId getID() {
        SharedPreferences sp = getSharedPreferences("userData", 0);
        //Get id or return the app primary color.
        String id = sp.getString("id", getString(R.string.appColor));
        String hash = sp.getString("hash", null);
        return new UserId(id, hash);
    }

    void updateActionBar() {
        int bgColor = Color.parseColor("#" + getID().getId());
        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(bgColor));
        //collapsingToolbar.setContentScrimColor(bgColor);
        if (collapsingToolbar != null) {
            collapsingToolbar.setBackgroundColor(bgColor);
        } else {
            toolbar.setBackgroundColor(bgColor);
        }
        if (menu != null) {
            menu.findItem(R.id.view_id).setTitle(getID().getId());
        }
        invalidateOptionsMenu();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String title = ((String) toolbar.getTitle());
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(title, icon, bgColor));
        }
    }

    void updateID(UserId idClass) {
        SharedPreferences userData = getSharedPreferences("userData", 0);
        String id = idClass.getId();
        String hash = idClass.getHash();
        SharedPreferences.Editor editor = userData.edit();
        editor.putString("id", id);
        editor.putString("hash", hash);
        editor.apply();
        updateActionBar();
        invalidateOptionsMenu();
    }

    boolean noID() {
        SharedPreferences userData = getSharedPreferences("userData", 0);
        String id = userData.getString("id", null);
        return (id == null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_addThread:
                goToThreadAdd();
                return true;

            case R.id.action_help:
                goToHelp();
                return true;
            case R.id.action_refresh_id:
                new requestID().execute();
                return true;
            case R.id.action_message_alarm:
                item.setChecked(!item.isChecked());
                if (item.isChecked()) {
                    startMessageAlarm();
                } else {
                    stopMessageAlarm();
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    class requestID extends AsyncTask<Void, Void, UserId> {
        protected UserId doInBackground(Void... voids) {
            try {
                return WebRequestHandler.getInstance().newID();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(UserId id) {
            super.onPostExecute(id);
            updateID(id);
        }
    }

    private void startMessageAlarm() {
        //Start alarm
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime()+AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);

        //Enable alarm on boot
        SharedPreferences userData = getSharedPreferences("userData", 0);
        SharedPreferences.Editor editor = userData.edit();
        editor.putBoolean("messageAlarm", true);
        editor.apply();
    }

    private void stopMessageAlarm() {
        //Stop alarm
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);

        //Disable alarm on boot
        SharedPreferences userData = getSharedPreferences("userData", 0);
        SharedPreferences.Editor editor = userData.edit();
        editor.putBoolean("messageAlarm", false);
        editor.apply();
    }
}

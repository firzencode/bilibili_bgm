package moe.ahaworks.bilibilibgm;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import moe.ahaworks.bilibilibgm.downlist.DownListFragment;
import moe.ahaworks.bilibilibgm.playback.PlaybackFragment;


public class MainActivity extends Activity implements
        View.OnClickListener,
        DownListFragment.IDownListFragmentListener,
        PlaybackFragment.IPlaybackFragmentListener {
    private final String TAG = "MainActivity";
    private BgmService mService;
    private boolean isServiceBound;

    private LauncherFragment mFragmentLauncher;
    private DownListFragment mFragmentDownList;
    private PlaybackFragment mFragmentPlayback;
    private SettingsFragment mFragmentSettings;

    private View mBtnDownloadList;
    private View mBtnPlaylist;
    private View mBtnLocalList;
    private View mBtnSystem;
    private View mBtnPlayback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.LOGD(TAG, "OnCreate");
        setContentView(R.layout.activity_main);

        mFragmentLauncher = new LauncherFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.layout_root, mFragmentLauncher);
        ft.commit();

        mBtnDownloadList = findViewById(R.id.btn_download_list);
        mBtnDownloadList.setOnClickListener(this);
        mBtnPlaylist = findViewById(R.id.btn_playlist);
        mBtnPlaylist.setOnClickListener(this);
        mBtnLocalList = findViewById(R.id.btn_local_list);
        mBtnLocalList.setOnClickListener(this);
        mBtnSystem = findViewById(R.id.btn_system);
        mBtnSystem.setOnClickListener(this);
        mBtnPlayback = findViewById(R.id.btn_playback);
        mBtnPlayback.setOnClickListener(this);

        Intent intent = new Intent(this, BgmService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.LOGD(TAG, "onDestroy");
        if (isServiceBound) {
            unbindService(mConnection);
            isServiceBound = false;
        }
        if (!mService.isPlaying()) {
            Intent intent = new Intent(this, BgmService.class);
            stopService(intent);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BgmService.BgmServiceBinder binder = (BgmService.BgmServiceBinder) service;
            mService = binder.getService();
            isServiceBound = true;
            Utils.LOGD(TAG, "Bgm service connected.");

            AsyncTask<Void, Void, Void> removeLauncherTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    FragmentManager fm = getFragmentManager();
                    if (mFragmentLauncher != null && mFragmentLauncher.isAdded()) {
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.setCustomAnimations(0, R.anim.fade_out);
                        ft.remove(mFragmentLauncher);
                        ft.commit();
                    }
                    mBtnDownloadList.performClick();
                    super.onPostExecute(aVoid);
                }
            };
            removeLauncherTask.execute();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            Utils.LOGD(TAG, "Bgm service disconnected.");
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_download_list:
                openDownList();
                break;
            case R.id.btn_playlist:
                openPlayList();
                break;
            case R.id.btn_local_list:
                openLocalList();
                break;
            case R.id.btn_system:
                openSystem();
                break;
            case R.id.btn_playback:
                openPlayback();
                break;
        }
    }

    private void openDownList() {
        FragmentManager fm = getFragmentManager();
        if (mFragmentDownList == null) {
            mFragmentDownList = new DownListFragment();
        }
        if (mFragmentDownList.isAdded() == false) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.layout_main, mFragmentDownList);
            ft.commit();
        }
    }

    private void openPlayList() {
        Toast.makeText(this, "(/・ω・＼)这个还没来得及做……", Toast.LENGTH_SHORT).show();
    }

    private void openLocalList() {
        Toast.makeText(this, "(｡・`ω´･)施工中……", Toast.LENGTH_SHORT).show();
    }

    private void openSystem() {
        FragmentManager fm = getFragmentManager();
        if (mFragmentSettings == null) {
            mFragmentSettings = new SettingsFragment();
        }
        if (mFragmentSettings.isAdded() == false) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.layout_main, mFragmentSettings);
            ft.commit();
        }
    }

    private void openPlayback() {
        FragmentManager fm = getFragmentManager();
        if (mFragmentPlayback == null) {
            mFragmentPlayback = new PlaybackFragment();
        }
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(R.anim.playback_in, 0);
        ft.add(R.id.layout_root, mFragmentPlayback);
        ft.commit();
        mService.bindListener(mFragmentPlayback);
    }

    private void closePlayback() {
        FragmentManager fm = getFragmentManager();
        if (mFragmentPlayback != null && mFragmentPlayback.isAdded()) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(0, R.anim.playback_out);
            ft.remove(mFragmentPlayback);
            ft.commit();
        }
        mService.unbindListener();
    }

    /* ----- DownList Fragment Callback ----- */

    @Override
    public void onRefreshClick() {
        LoadDatalistTask task = new LoadDatalistTask();
        task.execute();
    }

    @Override
    public void onListItemClick(MusicItem item) {
        mService.getCurPlaylist().clear();
        mService.getCurPlaylist().add(item);
        mService.playMusicItem(0);
    }

    @Override
    public void onListItemAddToPlay(MusicItem item) {
        mService.getCurPlaylist().add(item);
        mService.playMusicItem(mService.getCurPlaylist().size() - 1);
    }

    private class LoadDatalistTask extends AsyncTask<Void, Void, BgmService.DownListScanResult> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected BgmService.DownListScanResult doInBackground(Void... params) {
            return mService.scanDownlistFolder(Environment.getExternalStorageDirectory()
                    .getPath() + BgmService.DEFAULT_DOWNLOAD_FOLDER);
        }

        @Override
        protected void onPostExecute(BgmService.DownListScanResult result) {
            if (result.result == BgmService.ERR_NONE) {
                if (mFragmentDownList != null) {
                    mFragmentDownList.onDownListRefresh(result.list);
                }
            }
        }
    }

    /* ----- Playback Fragment Callback ----- */

    @Override
    public void onClosePlaybackFragment() {
        closePlayback();
    }

    @Override
    public ArrayList<MusicItem> onGetCurPlayList() {
        return mService.getCurPlaylist();
    }

    @Override
    public void onPlayPause() {
        mService.playPause();
    }

    @Override
    public MusicItem onRequestCurMusicItem() {
        if (mService.getCurPlayIndex() != BgmService.INVALIDATE_PLAY_INDEX) {
            return mService.getCurPlaylist().get(mService.getCurPlayIndex());
        } else {
            return null;
        }
    }

    @Override
    public void onSeekTo(int progress) {
        mService.seekTo(progress);
    }

    @Override
    public BgmService.LOOP_MODE onSwitchLoopMode() {
        BgmService.LOOP_MODE curLoopMode = mService.getLoopMode();
        switch (curLoopMode) {
            case NONE:
                curLoopMode = BgmService.LOOP_MODE.LOOP_ONE;
                break;
            case LOOP_ONE:
                curLoopMode = BgmService.LOOP_MODE.LOOP_LIST;
                break;
            case LOOP_LIST:
                curLoopMode = BgmService.LOOP_MODE.NONE;
                break;
        }

        mService.setLoopMode(curLoopMode);
        return curLoopMode;
    }

    @Override
    public boolean onSwitchRandomMode() {
        boolean curIsRandom = mService.getRandomEnable();
        mService.setRandomEnable(!curIsRandom);
        return !curIsRandom;
    }

    @Override
    public BgmService.LOOP_MODE onRequestLoopMode() {
        return mService.getLoopMode();
    }

    @Override
    public boolean onRequestRandomMode() {
        return mService.getRandomEnable();
    }

    @Override
    public void onMusicForward() {
        mService.playForward();
    }

    @Override
    public void onMusicBackward() {
        mService.playBackward();
    }

    @Override
    public int onRequestCurPlayIndex() {
        return mService.getCurPlayIndex();
    }

    @Override
    public void onPlayTarget(int index) {
        mService.playMusicItem(index);
    }

    @Override
    public long getTimerRemain() {
        return mService.getTimerRemain();
    }

    @Override
    public void setTimer(long time) {
        mService.startTimer(time);
    }

}

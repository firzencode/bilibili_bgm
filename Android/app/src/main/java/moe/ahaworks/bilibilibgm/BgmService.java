package moe.ahaworks.bilibilibgm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import org.apache.http.util.EncodingUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

public class BgmService extends Service implements MediaPlayer.OnCompletionListener {
    /* ----- Internal Class ----- */

    public class BgmServiceBinder extends Binder {
        BgmService getService() {
            return BgmService.this;
        }
    }

    public class DownListScanResult {
        public ArrayList<MusicItem> list;
        public int result;
        public String info;

        public DownListScanResult() {
            list = new ArrayList<>();
        }
    }

    public interface IBgmListener {
        void onUpdatePlayingTimeInfo(int curTime, int totalTime);

        void onUpdatePlayingItem(String title, String subTitle, int totalTime);
    }

    private class Mp4FindFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String filename) {
            return (filename.endsWith(".mp4"));
        }
    }

    public enum LOOP_MODE {
        NONE, LOOP_ONE, LOOP_LIST
    }

    /* ----- Properties ----- */

    private final String TAG = "BgmService";
    public static final String DEFAULT_DOWNLOAD_FOLDER = "/Android/data/tv.danmaku.bili/download";
    public static final int ERR_NONE = 0;
    public static final int ERR_FOLDER_NOT_FOUND = 1;
    public static final int ERR_READ_ENTRY_FILE = 2;
    public static final int ERR_READ_JSON = 3;
    public static final int INVALIDATE_PLAY_INDEX = -1;
    private final IBinder mBinder = new BgmServiceBinder();
    private final int UPDATE_PLAYING_TIME_INFO_DELAY = 200;
    private final int AUTO_PAUSE_TIMER_DELAY = 1000;

    private final int MSG_UPDATE_PLAYING_TIME_INFO = 101;
    private final int MSG_AUTO_PAUSE_TIMER_TICK = 102;

    private MediaPlayer mMediaPlayer;
    private ArrayList<MusicItem> mCurPlaylist;
    private int mCurPlayIndex = INVALIDATE_PLAY_INDEX;
    private IBgmListener mListener;
    private LOOP_MODE mLoopMode = LOOP_MODE.NONE;
    private boolean mIsRandom = false;
    private long mAutoPauseTimer = Long.MAX_VALUE;
    private BroadcastReceiver mBroadcastReceiver;

    private Handler mHandler;

    /* ----- System Override ----- */

    public BgmService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Utils.LOGD(TAG, "OnCreate");

        /* --- Init Service --- */

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);

        mCurPlaylist = new ArrayList<MusicItem>();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_PLAYING_TIME_INFO:
                        onHandleUpdatePlayingTimeInfo();
                        break;
                    case MSG_AUTO_PAUSE_TIMER_TICK:
                        mAutoPauseTimer -= AUTO_PAUSE_TIMER_DELAY;
                        if (mAutoPauseTimer <= 0) {
                            if (mCurPlayIndex != INVALIDATE_PLAY_INDEX) {
                                mMediaPlayer.pause();
                            }
                            mAutoPauseTimer = Long.MAX_VALUE;
                        } else {
                            mHandler.sendEmptyMessageDelayed(MSG_AUTO_PAUSE_TIMER_TICK, AUTO_PAUSE_TIMER_DELAY);
                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                    onHeadsetDisconnected();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mBroadcastReceiver, intentFilter);

        /* --- Foreground Service --- */

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Bilibili Bgm Player");
        builder.setContentText("播放器工作中");
        builder.setOngoing(true);
        builder.setShowWhen(false);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        startForeground(2801, notification);

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Utils.LOGD(TAG, "OnDestroy");
        mMediaPlayer.release();
        stopForeground(true);
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    /* ----- Implements ----- */

    @Override
    public void onCompletion(MediaPlayer mp) {
        playForward(false);
    }

    /* ----- Private Methods ----- */

    private void onHandleUpdatePlayingTimeInfo() {
        if (mListener != null) {
            int curPosition = 0;
            int totalTime = 0;
            if (mCurPlayIndex != INVALIDATE_PLAY_INDEX) {
                curPosition = mMediaPlayer.getCurrentPosition();
                totalTime = mMediaPlayer.getDuration();
            }
            mListener.onUpdatePlayingTimeInfo(curPosition, totalTime);
        }
        mHandler.removeMessages(MSG_UPDATE_PLAYING_TIME_INFO);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PLAYING_TIME_INFO, UPDATE_PLAYING_TIME_INFO_DELAY);
    }

    private void onHeadsetDisconnected() {
        if (mCurPlayIndex != INVALIDATE_PLAY_INDEX) {
            mMediaPlayer.pause();
        }
    }

    /* ----- Public Methods ----- */

    /**
     * @param folderPath
     * @return
     */
    public DownListScanResult scanDownlistFolder(String folderPath) {
        DownListScanResult res = new DownListScanResult();

        File rootFolder = new File(folderPath);
        if (rootFolder.exists() && rootFolder.isDirectory()) {
            for (File avFolder : rootFolder.listFiles()) {
                if (avFolder.isDirectory()) {
                    // av id folder
                    for (File pageFolder : avFolder.listFiles()) {
                        String entryPath = pageFolder.getAbsolutePath()
                                + "/entry.json";
                        // page folder
                        try {
                            File file = new File(entryPath);
                            FileInputStream fis = new FileInputStream(file);
                            int length = fis.available();
                            byte[] buffer = new byte[length];
                            fis.read(buffer);
                            String jsonRaw = EncodingUtils.getString(buffer, "UTF-8");
                            fis.close();

                            JSONTokener jsonParser = new JSONTokener(jsonRaw);
                            JSONObject obj = (JSONObject) jsonParser.nextValue();
                            String title = obj.getString("title");
                            long avid = obj.getLong("avid");
                            long time = obj.getLong("total_time_milli");
                            JSONObject pageData = obj.getJSONObject("page_data");
                            int partId = pageData.getInt("page");
                            String partTitle = pageData.getString("part");
                            String filePath = null;
                            for (File mp4File : pageFolder.listFiles(new Mp4FindFilter())) {
                                filePath = mp4File.getAbsolutePath();
                                Log.d("TEST", "path = " + filePath);
                            }
                            if (filePath != null) {
                                MusicItem item = new MusicItem(title, avid, time, partId, partTitle, filePath);
                                res.list.add(item);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            res.result = ERR_READ_ENTRY_FILE;
                            res.info = entryPath + " => " + e.getMessage();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            res.result = ERR_READ_JSON;
                            res.info = entryPath + " => " + e.getMessage();
                        }
                    }
                }
            }
        } else {
            res.result = ERR_FOLDER_NOT_FOUND;
            return res;
        }
        res.result = ERR_NONE;
        return res;
    }


    /* ----- Play Control ----- */

    public void playPause() {
        if (mCurPlayIndex == INVALIDATE_PLAY_INDEX) {
            if (mCurPlaylist.size() > 0) {
                playMusicItem(0);
            }
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            } else {
                mMediaPlayer.start();
            }
        }
    }

    public void playMusicItem(int index) {
        mCurPlayIndex = index;
        MusicItem item = mCurPlaylist.get(index);

        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(item.mFilePath);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            if (mListener != null) {
                mListener.onUpdatePlayingItem(item.mTitle, item.mPartTitle, mMediaPlayer.getDuration());
            }
        } catch (IOException e) {
            e.printStackTrace();
            playForward();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            playForward();
        }
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void seekTo(int progress) {
        if (mCurPlayIndex != INVALIDATE_PLAY_INDEX) {
            mMediaPlayer.seekTo(progress);
        }
    }

    private void playForward(boolean manually) {
        if (manually) {
            if (mCurPlayIndex != INVALIDATE_PLAY_INDEX && mCurPlaylist.size() > 0) {
                if (mCurPlayIndex < mCurPlaylist.size() - 1) {
                    playMusicItem(mCurPlayIndex + 1);
                } else {
                    playMusicItem(0);
                }
            }
        } else {
            if (mCurPlayIndex != INVALIDATE_PLAY_INDEX && mCurPlaylist.size() > 0) {
                switch (mLoopMode) {
                    case NONE:
                        if (mCurPlayIndex < mCurPlaylist.size() - 1) {
                            playMusicItem(mCurPlayIndex + 1);
                        } else {
                            mCurPlayIndex = INVALIDATE_PLAY_INDEX;
                            if (mListener != null) {
                                mListener.onUpdatePlayingItem("", "", 0);
                            }
                        }
                        break;
                    case LOOP_ONE:
                        playMusicItem(mCurPlayIndex);
                        break;
                    case LOOP_LIST:
                        if (mCurPlayIndex < mCurPlaylist.size() - 1) {
                            playMusicItem(mCurPlayIndex + 1);
                        } else {
                            playMusicItem(0);
                        }
                        break;
                }
            }
        }
    }

    public void playForward() {
        playForward(true);
    }

    public void playBackward() {
        if (mCurPlayIndex != INVALIDATE_PLAY_INDEX && mCurPlaylist.size() > 0) {
            if (mMediaPlayer.getCurrentPosition() > 3000) {
                playMusicItem(mCurPlayIndex);
            } else {
                if (mCurPlayIndex > 0) {
                    playMusicItem(mCurPlayIndex - 1);
                } else {
                    playMusicItem(mCurPlaylist.size() - 1);
                }
            }
        }
    }

    /* ----- Listener ----- */

    public void bindListener(IBgmListener listener) {
        mListener = listener;
        mHandler.removeMessages(MSG_UPDATE_PLAYING_TIME_INFO);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PLAYING_TIME_INFO, UPDATE_PLAYING_TIME_INFO_DELAY);
    }

    public void unbindListener() {
        mListener = null;
    }

    /* ----- Play list ----- */

    public int getCurPlayIndex() {
        return mCurPlayIndex;
    }

    @Deprecated
    public ArrayList<MusicItem> getCurPlaylist() {
        return mCurPlaylist;
    }

    /* ------ Loop & Random ----- */

    public LOOP_MODE getLoopMode() {
        return mLoopMode;
    }

    public void setLoopMode(LOOP_MODE mode) {
        mLoopMode = mode;
    }

    public boolean getRandomEnable() {
        return mIsRandom;
    }

    public void setRandomEnable(boolean value) {
        mIsRandom = value;
    }

    /* ----- Timer ----- */
    public void startTimer(long timeMs) {
        if (timeMs == 0) {
            mHandler.removeMessages(MSG_AUTO_PAUSE_TIMER_TICK);
            mAutoPauseTimer = Long.MAX_VALUE;
        } else {
            mAutoPauseTimer = timeMs;
            mHandler.removeMessages(MSG_AUTO_PAUSE_TIMER_TICK);
            mHandler.sendEmptyMessageDelayed(MSG_AUTO_PAUSE_TIMER_TICK, AUTO_PAUSE_TIMER_DELAY);
        }
    }

    public long getTimerRemain() {
        return mAutoPauseTimer;
    }
}

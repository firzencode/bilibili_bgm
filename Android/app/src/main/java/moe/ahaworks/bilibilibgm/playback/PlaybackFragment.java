package moe.ahaworks.bilibilibgm.playback;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;

import moe.ahaworks.bilibilibgm.BgmService;
import moe.ahaworks.bilibilibgm.MusicItem;
import moe.ahaworks.bilibilibgm.R;
import moe.ahaworks.bilibilibgm.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaybackFragment extends Fragment
        implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,
        BgmService.IBgmListener,
        PlaybackListAdapter.IPlaybackListAdapterListener,
        AdapterView.OnItemClickListener {

    public interface IPlaybackFragmentListener {
        void onClosePlaybackFragment();

        ArrayList<MusicItem> onGetCurPlayList();

        void onPlayPause();

        MusicItem onRequestCurMusicItem();

        void onSeekTo(int progress);

        BgmService.LOOP_MODE onSwitchLoopMode();

        boolean onSwitchRandomMode();

        BgmService.LOOP_MODE onRequestLoopMode();

        boolean onRequestRandomMode();

        void onMusicForward();

        void onMusicBackward();

        int onRequestCurPlayIndex();

        void onPlayTarget(int index);

        long getTimerRemain();

        void setTimer(long time);
    }

    private ListView mList;
    private PlaybackListAdapter mListAdapter;
    private ArrayList<MusicItem> mCurPlayList;

    private SeekBar mSeekBar;
    private TextView mTvCurTitle;
    private TextView mTvCurSubtitle;
    private TextView mTvTime;
    private TextView mTvTimeTotal;

    private ImageView mIvLoop;
    private ImageView mIvRandom;

    private boolean mIsSeekBarDragging = false;

    public PlaybackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    // handle back button
                    ((IPlaybackFragmentListener) getActivity()).onClosePlaybackFragment();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playback, container, false);
        mList = (ListView) view.findViewById(R.id.playback_list);
        mCurPlayList = new ArrayList<MusicItem>();
        mListAdapter = new PlaybackListAdapter(getActivity(), R.layout.item_playback_list, mCurPlayList, this);
        mList.setAdapter(mListAdapter);
        mList.setOnItemClickListener(this);

        View btnClose = view.findViewById(R.id.playback_btn_close);
        btnClose.setOnClickListener(this);

        View btnTimer = view.findViewById(R.id.playback_btn_timer);
        btnTimer.setOnClickListener(this);

        View btnPlayPause = view.findViewById(R.id.playback_btn_play_pause);
        btnPlayPause.setOnClickListener(this);

        View btnBackward = view.findViewById(R.id.playback_btn_backward);
        btnBackward.setOnClickListener(this);

        View btnForward = view.findViewById(R.id.playback_btn_forward);
        btnForward.setOnClickListener(this);

        mIvLoop = (ImageView) view.findViewById(R.id.playback_btn_loop);
        mIvLoop.setOnClickListener(this);

        mIvRandom = (ImageView) view.findViewById(R.id.playback_btn_random);
        mIvRandom.setOnClickListener(this);

        mSeekBar = (SeekBar) view.findViewById(R.id.playback_cur_seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);

        mTvCurTitle = (TextView) view.findViewById(R.id.playback_cur_title);
        mTvCurSubtitle = (TextView) view.findViewById(R.id.playback_cur_subtitle);
        mTvTime = (TextView) view.findViewById(R.id.playback_cur_time);
        mTvTimeTotal = (TextView) view.findViewById(R.id.playback_cur_time_total);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mCurPlayList.clear();
        mCurPlayList.addAll(((IPlaybackFragmentListener) getActivity()).onGetCurPlayList());
        mListAdapter.notifyDataSetChanged();
        MusicItem item = ((IPlaybackFragmentListener) getActivity()).onRequestCurMusicItem();
        if (item != null) {
            onRefreshItem(item.mTitle, item.mPartTitle, (int) item.mTime);
        } else {
            onRefreshItem("", "", 0);
        }

        BgmService.LOOP_MODE curMode = ((IPlaybackFragmentListener) getActivity()).onRequestLoopMode();
        switch (curMode) {

            case NONE:
                mIvLoop.setImageResource(R.mipmap.btn_loop_none);
                break;
            case LOOP_ONE:
                mIvLoop.setImageResource(R.mipmap.btn_loop_one);
                break;
            case LOOP_LIST:
                mIvLoop.setImageResource(R.mipmap.btn_loop_list);
                break;
        }

        boolean isRandom = ((IPlaybackFragmentListener) getActivity()).onRequestRandomMode();

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playback_btn_close:
                onClickClose();
                break;
            case R.id.playback_btn_timer:
                onClickTimer();
                break;
            case R.id.playback_btn_play_pause:
                onClickPlayPause();
                break;
            case R.id.playback_btn_backward:
                onClickBackward();
                break;
            case R.id.playback_btn_forward:
                onClickForward();
                break;
            case R.id.playback_btn_loop:
                onClickSwitchLoopMode();
                break;
            case R.id.playback_btn_random:
                onClickSwitchRandomMode();
                break;
        }
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mIsSeekBarDragging = true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && mIsSeekBarDragging) {
            mTvTime.setText(Utils.ConvertTime(progress));
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mIsSeekBarDragging = false;
        ((IPlaybackFragmentListener) getActivity()).onSeekTo(seekBar.getProgress());
    }


    private void onClickClose() {
        ((IPlaybackFragmentListener) getActivity()).onClosePlaybackFragment();
    }

    private void onClickTimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("定时关闭");
        View contentView = getActivity().getLayoutInflater().inflate(R.layout.dialog_timer, null);
        final TimePicker timePicker = (TimePicker) contentView.findViewById(R.id.timer_time_picker);
        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(1);
        timePicker.setCurrentMinute(0);

        TextView tvTimerState = (TextView) contentView.findViewById(R.id.timer_tv_state);
        long remainTime = ((IPlaybackFragmentListener) getActivity()).getTimerRemain();
        if (remainTime == Long.MAX_VALUE) {
            tvTimerState.setText("未启动");
        } else {
            tvTimerState.setText("剩余时间：" + Utils.ConvertTime(remainTime));
        }
        builder.setView(contentView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), "将在" + timePicker.getCurrentHour() + "小时" + timePicker.getCurrentMinute() + "分钟之后停止播放", Toast.LENGTH_SHORT).show();
                long hour = timePicker.getCurrentHour() * 60 * 60 * 1000;
                long minute = timePicker.getCurrentMinute() * 60 * 1000;
                ((IPlaybackFragmentListener) getActivity()).setTimer(hour + minute);

            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), "停止计时器", Toast.LENGTH_SHORT).show();
                ((IPlaybackFragmentListener) getActivity()).setTimer(0);
            }
        });
        builder.show();
    }

    private void onClickPlayPause() {
        ((IPlaybackFragmentListener) getActivity()).onPlayPause();
    }

    private void onClickSwitchLoopMode() {
        BgmService.LOOP_MODE curMode = ((IPlaybackFragmentListener) getActivity()).onSwitchLoopMode();
        switch (curMode) {

            case NONE:
                mIvLoop.setImageResource(R.mipmap.btn_loop_none);
                break;
            case LOOP_ONE:
                mIvLoop.setImageResource(R.mipmap.btn_loop_one);
                break;
            case LOOP_LIST:
                mIvLoop.setImageResource(R.mipmap.btn_loop_list);
                break;
        }
    }

    private void onClickSwitchRandomMode() {
        boolean curIsRandom = ((IPlaybackFragmentListener) getActivity()).onSwitchRandomMode();
        Toast toast = Toast.makeText(getActivity(), "随机播放还没做……点了也没用（摊手", Toast.LENGTH_SHORT);
        toast.show();
    }

    private void onClickForward() {
        ((IPlaybackFragmentListener) getActivity()).onMusicForward();
    }

    private void onClickBackward() {
        ((IPlaybackFragmentListener) getActivity()).onMusicBackward();
    }

    /* ----- Array Adapter Callback ----- */

    @Override
    public int getCurPlayIndex() {
        return ((IPlaybackFragmentListener) getActivity()).onRequestCurPlayIndex();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((IPlaybackFragmentListener) getActivity()).onPlayTarget(position);
    }

    /* ----- MainActivity Callback ----- */

    public void onRefreshItem(String title, String subTitle, int totalTime) {
        mTvCurTitle.setText(title);
        mTvCurSubtitle.setText(subTitle);
        mSeekBar.setMax(totalTime);
    }

    @Override
    public void onUpdatePlayingTimeInfo(int curTime, int totalTime) {
        if (mTvTime != null && mTvTimeTotal != null) {
            mTvTimeTotal.setText(Utils.ConvertTime(totalTime));
            if (mIsSeekBarDragging == false) {
                mTvTime.setText(Utils.ConvertTime(curTime));
                mSeekBar.setProgress(curTime);
            }
        }
    }

    @Override
    public void onUpdatePlayingItem(String title, String subTitle, int totalTime) {
        onRefreshItem(title, subTitle, totalTime);
        mListAdapter.notifyDataSetChanged();
    }
}

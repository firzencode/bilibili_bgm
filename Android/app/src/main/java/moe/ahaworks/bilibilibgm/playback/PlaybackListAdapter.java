package moe.ahaworks.bilibilibgm.playback;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import moe.ahaworks.bilibilibgm.MusicItem;
import moe.ahaworks.bilibilibgm.R;
import moe.ahaworks.bilibilibgm.Utils;

public class PlaybackListAdapter extends ArrayAdapter<MusicItem> {

    public interface IPlaybackListAdapterListener {
        int getCurPlayIndex();
    }

    private class ItemHolder {
        TextView mTvIndex;
        TextView mTvTitle;
        TextView mTvSubTitle;
        TextView mTvTime;
    }

    private int mResourceId;
    private LayoutInflater mInflater;
    private IPlaybackListAdapterListener mListener;

    public PlaybackListAdapter(Context context, int resource, List<MusicItem> objects, IPlaybackListAdapterListener listener) {
        super(context, resource, objects);
        mResourceId = resource;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemHolder holder;
        if (null == convertView) {
            convertView = mInflater.inflate(mResourceId, null);
            holder = new ItemHolder();
            holder.mTvIndex = (TextView) convertView.findViewById(R.id.playback_list_item_index);
            holder.mTvTitle = (TextView) convertView.findViewById(R.id.playback_list_item_title);
            holder.mTvSubTitle = (TextView) convertView.findViewById(R.id.playback_list_item_subtitle);
            holder.mTvTime = (TextView) convertView.findViewById(R.id.playback_list_item_time);
            convertView.setTag(holder);
        } else {
            holder = (ItemHolder) convertView.getTag();
        }

        MusicItem item = getItem(position);
        if (mListener.getCurPlayIndex() == position) {
            holder.mTvIndex.setText(">>");
        } else {
            holder.mTvIndex.setText((position + 1) + ".");
        }
        holder.mTvTitle.setText(item.mTitle);
        holder.mTvSubTitle.setText(item.mPartTitle);
        if (item.mTime != 0) {
            holder.mTvTime.setText(Utils.ConvertTime(item.mTime));
        }

        return convertView;
    }
}

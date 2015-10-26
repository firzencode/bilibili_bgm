package moe.ahaworks.bilibilibgm.downlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import moe.ahaworks.bilibilibgm.MusicItem;
import moe.ahaworks.bilibilibgm.R;

public class DownListAdapter extends ArrayAdapter<MusicItem> {

    private class ItemHolder {
        TextView mTvTitle;
        TextView mTvPageTitle;
        View mViewAddToPlay;
    }

    private int mResourceId;
    private LayoutInflater mInflater;
    private View.OnClickListener mOnClickListener;

    public DownListAdapter(Context context, int resource, List<MusicItem> objects, View.OnClickListener onClickListener) {
        super(context, resource, objects);
        mResourceId = resource;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mOnClickListener = onClickListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemHolder holder;
        if (null == convertView) {
            convertView = mInflater.inflate(mResourceId, null);
            holder = new ItemHolder();
            holder.mTvTitle = (TextView) convertView.findViewById(R.id.downlist_item_title);
            holder.mTvPageTitle = (TextView) convertView.findViewById(R.id.downlist_item_page_title);
            holder.mViewAddToPlay = convertView.findViewById(R.id.downlist_item_add_to_play);
            convertView.setTag(holder);
        } else {
            holder = (ItemHolder) convertView.getTag();
        }

        MusicItem item = getItem(position);
        holder.mTvTitle.setText(item.mTitle);
        holder.mTvPageTitle.setText(item.mPartTitle);
        holder.mViewAddToPlay.setOnClickListener(mOnClickListener);
        holder.mViewAddToPlay.setTag(position);
        return convertView;
    }
}

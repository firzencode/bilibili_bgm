package moe.ahaworks.bilibilibgm.downlist;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import moe.ahaworks.bilibilibgm.MusicItem;
import moe.ahaworks.bilibilibgm.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class DownListFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    public interface IDownListFragmentListener {
        void onRefreshClick();

        void onListItemClick(MusicItem item);

        void onListItemAddToPlay(MusicItem item);
    }

    private ListView mList;
    private DownListAdapter mListAdapter;
    private ArrayList<MusicItem> mDownItemList;

    public DownListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downlist, container, false);

        View btnRefresh = view.findViewById(R.id.downlist_btn_refresh);
        btnRefresh.setOnClickListener(this);

        mList = (ListView) view.findViewById(R.id.downlist_list_main);
        mDownItemList = new ArrayList<>();
        mListAdapter = new DownListAdapter(getActivity(), R.layout.item_datalist, mDownItemList, this);
        mList.setAdapter(mListAdapter);
        mList.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((IDownListFragmentListener) getActivity()).onRefreshClick();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.downlist_btn_refresh:
                ((IDownListFragmentListener) getActivity()).onRefreshClick();
                break;
            case R.id.downlist_item_add_to_play:
                int position = (int) v.getTag();
                ((IDownListFragmentListener) getActivity()).onListItemAddToPlay(mDownItemList.get(position));
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((IDownListFragmentListener) getActivity()).onListItemClick(mDownItemList.get(position));
    }

    /* --- Activity Callback --- */

    public void onDownListRefresh(ArrayList<MusicItem> itemList) {
        mDownItemList.clear();
        mDownItemList.addAll(itemList);
        mListAdapter.notifyDataSetChanged();
    }


}

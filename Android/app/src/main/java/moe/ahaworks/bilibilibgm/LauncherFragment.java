package moe.ahaworks.bilibilibgm;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class LauncherFragment extends Fragment {
    public LauncherFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_launcher, container, false);
        TextView version = (TextView) view.findViewById(R.id.launcher_tv_version);
        version.setText("ver " + Utils.getVertsionInfo(getActivity()));
        return view;
    }
}

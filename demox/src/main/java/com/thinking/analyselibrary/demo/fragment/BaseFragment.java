package com.thinking.analyselibrary.demo.fragment;

import androidx.fragment.app.Fragment;

import com.thinking.analyselibrary.ThinkingDataTrackFragmentAppViewScreen;

@ThinkingDataTrackFragmentAppViewScreen
public class BaseFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }
}

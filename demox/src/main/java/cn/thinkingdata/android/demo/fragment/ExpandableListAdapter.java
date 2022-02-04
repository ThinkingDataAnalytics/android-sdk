package cn.thinkingdata.android.demo.fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import cn.thinkingdata.android.ThinkingExpandableListViewItemTrackProperties;
import cn.thinkingdata.android.demo.R;

import org.json.JSONObject;

/**
 * 通过实现 ThinkingExpandableListViewItemTrackProperties 实现自动采集事件中的自定义属性设置
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter
        implements ThinkingExpandableListViewItemTrackProperties {
    private static final String TAG = "NormalExpandableListAda";
    private String[] groupData;
    private String[][] childData;
    private OnGroupExpandedListener mOnGroupExpandedListener;

    public ExpandableListAdapter(String[] groupData, String[][] childData) {
        this.groupData = groupData;
        this.childData = childData;
    }

    public void setOnGroupExpandedListener(OnGroupExpandedListener onGroupExpandedListener) {
        mOnGroupExpandedListener = onGroupExpandedListener;
    }

    @Override
    public int getGroupCount() {
        return groupData.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return childData[groupPosition].length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupData[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return childData[groupPosition][childPosition];
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View
            convertView, ViewGroup parent) {
        GroupViewHolder groupViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expand_group_normal, parent, false);
            groupViewHolder = new GroupViewHolder();
            groupViewHolder.tvTitle = (TextView) convertView.findViewById(R.id.label_group_normal);
            convertView.setTag(groupViewHolder);
        } else {
            groupViewHolder = (GroupViewHolder) convertView.getTag();
        }
        groupViewHolder.tvTitle.setText(groupData[groupPosition]);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View
            convertView, ViewGroup parent) {
        ChildViewHolder childViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expand_child, parent, false);
            childViewHolder = new ChildViewHolder();
            childViewHolder.tvTitle = (TextView) convertView.findViewById(R.id.label_expand_child);
            convertView.setTag(childViewHolder);
        } else {
            childViewHolder = (ChildViewHolder) convertView.getTag();
        }
        childViewHolder.tvTitle.setText(childData[groupPosition][childPosition]);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        Log.d(TAG, "onGroupExpanded() called with: groupPosition = [" + groupPosition + "]");
        if (mOnGroupExpandedListener != null) {
            mOnGroupExpandedListener.onGroupExpanded(groupPosition);
        }
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        Log.d(TAG, "onGroupCollapsed() called with: groupPosition = [" + groupPosition + "]");
    }

    @Override
    public JSONObject getThinkingChildItemTrackProperties(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public JSONObject getThinkingGroupItemTrackProperties(int groupPosition) {
        return null;
    }

    private static class GroupViewHolder {
        TextView tvTitle;
    }

    private static class ChildViewHolder {
        TextView tvTitle;
    }
    public interface OnGroupExpandedListener {
        /**
         * 分组展开
         *
         * @param groupPosition 分组的位置
         */
        void onGroupExpanded(int groupPosition);
    }
}



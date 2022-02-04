package cn.thinkingdata.android.demo.fragment;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import cn.thinkingdata.android.demo.R;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.myViewHodler> {
        private Context context;
        private ArrayList<RecyclerViewEntity> goodsEntityList;

        //创建构造函数
        public RecyclerViewAdapter(Context context, ArrayList<RecyclerViewEntity> goodsEntityList) {
            this.context = context;//上下文
            this.goodsEntityList = goodsEntityList;//实体类数据ArrayList
        }

        /**
         * 创建viewhodler，相当于listview中getview中的创建view和viewhodler
         *
         * @param parent
         * @param viewType
         * @return
         */
        @Override
        public myViewHodler onCreateViewHolder(ViewGroup parent, int viewType) {
            //创建自定义布局
            View itemView = View.inflate(context, R.layout.item_recyclerview, null);
            return new myViewHodler(itemView);
        }

        /**
         * 绑定数据，数据与view绑定
         *
         * @param holder
         * @param position
         */
        @Override
        public void onBindViewHolder(myViewHodler holder, int position) {
            //根据点击位置绑定数据
            RecyclerViewEntity data = goodsEntityList.get(position);
            holder.mItemGoodsName.setText(data.goodsName);//获取实体类中的name字段并设置
            holder.mItemGoodsPrice.setText(data.goodsPrice);//获取实体类中的price字段并设置

        }

        /**
         * 得到总条数
         *
         * @return
         */
        @Override
        public int getItemCount() {
            return goodsEntityList.size();
        }

        class myViewHodler extends RecyclerView.ViewHolder {
            private TextView mItemGoodsName;
            private TextView mItemGoodsPrice;

            public myViewHodler(View itemView) {
                super(itemView);
                mItemGoodsName = itemView.findViewById(R.id.item_name);
                mItemGoodsPrice = itemView.findViewById(R.id.item_price);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Toast.makeText(context,"点击了xxx",Toast.LENGTH_SHORT).show();
                        if(onItemClickListener != null){
                            onItemClickListener.OnItemClick(v, goodsEntityList.get(getLayoutPosition()));
                        }
                    }
                });

            }
        }

        /**
         * 设置item的监听事件的接口
         */
        public interface OnItemClickListener {
            /**
             * 接口中的点击每一项的实现方法，参数自己定义
             *
             * @param view 点击的item的视图
             * @param data 点击的item的数据
             */
            public void OnItemClick(View view, RecyclerViewEntity data);
        }

        //需要外部访问，所以需要设置set方法，方便调用
        private OnItemClickListener onItemClickListener;

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }


}

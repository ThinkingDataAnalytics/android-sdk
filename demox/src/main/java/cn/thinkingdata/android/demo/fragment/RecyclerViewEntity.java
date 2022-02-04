package cn.thinkingdata.android.demo.fragment;

import java.io.Serializable;

public class RecyclerViewEntity implements Serializable {
        public String imgPath; // 图片地址
        public String goodsName; // 货物名称
        public String goodsPrice;//货物价格

        public RecyclerViewEntity() {
        }

        public RecyclerViewEntity(String imgPath, String goodsName, String goodsPrice) {
            this.imgPath = imgPath;
            this.goodsName = goodsName;
            this.goodsPrice = goodsPrice;
        }

        public void setGoodsName(String goodsName) {
            this.goodsName = goodsName;
        }

        public void setGoodsPrice(String goodsPrice) {
            this.goodsPrice = goodsPrice;
        }

        @Override
        public String toString() {
            return "GoodsEntity{" +
                    "imgPath='" + imgPath + '\'' +
                    ", goodsName='" + goodsName + '\'' +
                    ", goodsPrice='" + goodsPrice + '\'' +
                    '}';
        }
}

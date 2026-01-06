package cn.thinkingdata.core.network;

import java.io.IOException;

public interface Call{

    TDNetResponse execute();

    void enqueue(TEHttpCallback responseCallback);

    interface Factory {
        Call newCall(Request request);
    }

}

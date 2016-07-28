package com.feifan.sampling.main.request;

import com.feifan.sampling.main.model.IsConnectedModel;
import com.libs.base.model.BaseJsonBean;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by mengmeng on 16/6/20.
 * 这个接口是判断服务是不是可以使用的
 */
public interface IsConnetedInterface {
    String IS_CONNNECTED_PATH = "/api/v1/devices";
    String NAME = "name";

    @GET(IS_CONNNECTED_PATH)
    Call<BaseJsonBean<IsConnectedModel>> isConnected(
            @Query(NAME) String name);
}
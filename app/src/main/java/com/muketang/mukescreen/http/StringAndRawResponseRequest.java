package com.muketang.mukescreen.http;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

public class StringAndRawResponseRequest extends StringRequest {

    private NetworkResponse response;

    public StringAndRawResponseRequest(int method, String url, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }


    public NetworkResponse getResponse() {
        return this.response;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        this.response = response;
        return super.parseNetworkResponse(response);
    }
}

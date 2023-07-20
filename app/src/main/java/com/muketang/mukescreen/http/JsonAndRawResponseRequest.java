package com.muketang.mukescreen.http;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class JsonAndRawResponseRequest extends JsonObjectRequest {

    private NetworkResponse response;

    public JsonAndRawResponseRequest(int method, String url, @Nullable JSONObject jsonRequest, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    public JsonAndRawResponseRequest(String url, @Nullable JSONObject jsonRequest, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
    }

    public NetworkResponse getResponse() {
        return this.response;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        this.response = response;
        return super.parseNetworkResponse(response);
    }
}

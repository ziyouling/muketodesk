package com.muketang.mukescreen;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WifiHttpD extends NanoHTTPD {

    private  IHttpLisenter lisenter;
    public WifiHttpD(IHttpLisenter lisenter) throws IOException {
        super(8080);
        this.lisenter = lisenter;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1'></head><body><h1>WIFI设置</h1>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("ssid") == null || parms.get("password") == null) {
            msg += "<form action='?' method='get'>\n  <p>SSID:: <input type='text' name='ssid'></p>\n <p>密码: <input type='text' name='password'></p>  <p><input type='submit' ></p>" + "</form>\n";
        } else {
            if(lisenter != null)
            {
                String result = lisenter.onHttpResponse(parms);
                msg += result;
            }
        }
        return newFixedLengthResponse(msg + "</body></html>\n");
    }

}

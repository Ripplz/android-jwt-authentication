package com.segunfamisa.sample.jwt.network;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkRequest {
    private static final String BASE_URL = "http://192.168.10.3:3001/"; //"http://localhost:3001";

    private Callback mCallback;

    private OkHttpClient mClient;

    public NetworkRequest() {
        mClient = new OkHttpClient();
    }

    /**
     * Sets the callback for the network request
     * @param callback
     */
    private void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * Login
     *
     * @param username - username
     * @param password - password
     * @param callback - callback
     */
    public void doLogin(@NonNull String username,
                        @NonNull String password,
                        Callback callback) {
        setCallback(callback);

        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);

        String loginUrl = BASE_URL + "sessions/create";
        doPostRequest(loginUrl, params, callback);
    }

    /**
     * Sign up
     *
     * @param username - username
     * @param password - password
     * @param profileColor - theme color for the user profile
     * @param callback - callback
     */
    public void doSignUp(@NonNull String username,
                         @NonNull String password,
                         @NonNull String profileColor,
                         @Nullable Callback callback) {
        setCallback(callback);

        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("extra", profileColor);

        String signUpUrl = BASE_URL + "users";
        doPostRequest(signUpUrl, params, callback);
    }

    /**
     * Execute post request
     *
     * @param url
     * @param params
     * @param callback
     */
    private void doPostRequest(@NonNull String url, @NonNull Map<String, String> params,
                               @Nullable final Callback callback) {
        HttpUrl httpUrl = HttpUrl.parse(url);

        FormBody.Builder bodyBuilder = new FormBody.Builder();
        for (String string : params.keySet()) {
            bodyBuilder.addEncoded(string, params.get(string));
        }

        Request request = new Request.Builder()
                .url(httpUrl)
                .post(bodyBuilder.build())
                .build();

        doRequest(request, callback);
    }

    /**
     * Makes request and fires callback as at when due
     *
     * @param request
     * @param callback
     */
    private void doRequest(@NonNull Request request, final Callback callback) {
        mClient.newCall(request)
                .enqueue(new okhttp3.Callback() {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    @Override
                    public void onFailure(Call call, final IOException e) {
                        if (callback != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError(e.toString());
                                }
                            });
                        }
                    }

                    @Override
                    public void onResponse(final Call call, final Response response) {
                        if(callback != null) {
                            try {
                                final String stringResponse = response.body().string();
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Object res = buildObjectFromResponse(stringResponse,
                                                callback.type());
                                        if (res != null) {
                                            callback.onResponse(res);
                                        } else {
                                            callback.onError(stringResponse);
                                        }
                                    }
                                });
                            } catch (final IOException ioe) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onError(ioe.toString());
                                    }
                                });
                            }
                        }
                    }
                });
    }

//    private void doGetRequestNoAuth(@NonNull String url, @NonNull Map<String, String> params, String tag, Callback callback) {
//        url = url + buildGetParamsString(url, params);
//        setCallback(callback);
//
//        HttpUrl httpUrl = HttpUrl.parse(url);
//
//        Request.Builder requestBuilder = new Request.Builder();
//        requestBuilder.get()
//                .header()
//    }
//
//    private void doGetRequestWithAuth() {}

    private Object buildObjectFromResponse(String response, Class cls) {
        if (cls == String.class) {
            return response;
        } else {
            try {
                return new Gson().fromJson(response, cls);
            } catch (JsonSyntaxException jse) {
                return null;
            }
        }
    }

    private String buildGetParamsString(String url, Map<String, String> params) {
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
        for (String key : params.keySet()) {
            urlBuilder.addQueryParameter(key, params.get(key));
        }
        return urlBuilder.toString();
    }

    public interface Callback<T> {
        void onResponse(@NonNull T response);
        void onError(String error);
        Class<T> type();
    }

    public interface ApiResponse {
        String string();
    }
}

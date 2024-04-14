package com.janboerman.f2pstarassist.plugin;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.janboerman.f2pstarassist.common.CrashedStar;
import com.janboerman.f2pstarassist.common.web.CrashedStarAdapater;
import com.janboerman.f2pstarassist.common.web.StarTypes;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NewStarClient {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(CrashedStar.class, new CrashedStarAdapater())
            .create();

    private final StarAssistConfig config;
    private final OkHttpClient httpClient;

    public NewStarClient(StarAssistConfig config, OkHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public CompletableFuture<List<CrashedStar>> requestStars() {

        String url = config.httpUrl() + "/stars";
        Request request = new Request.Builder().url(url).build();

        final CompletableFuture<List<CrashedStar>> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ResponseException(call, e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new ResponseException(call,
                                "WebServer answered with response code: " + response.code()));
                        return;
                    }

                    ResponseBody body = response.body();

                    List<CrashedStar> stars = gson.fromJson(body.charStream(), StarTypes.STAR_LIST);
                    future.complete(stars);
                } finally {
                    response.close();
                }
            }
        });

    }
}

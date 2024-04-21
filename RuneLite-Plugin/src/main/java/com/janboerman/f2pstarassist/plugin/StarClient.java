package com.janboerman.f2pstarassist.plugin;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.janboerman.f2pstarassist.plugin.model.CrashedStar;
import com.janboerman.f2pstarassist.plugin.model.DeletionMethod;
import com.janboerman.f2pstarassist.plugin.model.StarKey;
import com.janboerman.f2pstarassist.plugin.model.StarTier;
import com.janboerman.f2pstarassist.plugin.web.CrashedStarAdapater;
import com.janboerman.f2pstarassist.plugin.web.StarTypes;
import com.janboerman.f2pstarassist.plugin.web.ResponseException;

import net.runelite.api.FriendsChatRank;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StarClient {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CrashedStar.class, new CrashedStarAdapater())
            .create();
    private static final MediaType APPLICATION_JSON = MediaType.get("application/json");

    private static final String RANK_HEADER = "F2P-StarHunt-Rank";

    private final StarAssistConfig config;
    private final OkHttpClient httpClient;

    @Inject
    public StarClient(StarAssistConfig config, OkHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public CompletableFuture<List<CrashedStar>> requestStars(FriendsChatRank rank) {

        String url = config.httpUrl() + "/stars";
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (StarAssistPlugin.isRanked(rank)) {
            requestBuilder.header(RANK_HEADER, rank.name());
        }
        Request request = requestBuilder.build();

        final CompletableFuture<List<CrashedStar>> future = new CompletableFuture<>();
        sendRequest(future, request, responseBody -> {
            try (Reader reader = responseBody.charStream()) {
                return GSON.fromJson(reader, StarTypes.STAR_LIST);
            }
        });
        return future;
    }

    public CompletableFuture<Long> postStar(CrashedStar star) {
        String url = config.httpUrl() + "/stars";

        RequestBody body = RequestBody.create(APPLICATION_JSON, GSON.toJson(star));
        Request request = new Request.Builder().url(url).post(body).build();

        final CompletableFuture<Long> future = new CompletableFuture<>();
        sendRequest(future, request, responseBody -> {
            try (Reader reader = responseBody.charStream()) {
                return GSON.fromJson(reader, Long.class);
            }
        });
        return future;
    }

    public CompletableFuture<Void> updateStarTier(StarKey key, StarTier newTier) {
        String url = config.httpUrl() + "/stars/" + key.getWorld() + "/" + key.getLocation();

        RequestBody body = RequestBody.create(APPLICATION_JSON, GSON.toJson(newTier.getSize()));
        Request request = new Request.Builder().url(url).patch(body).build();

        final CompletableFuture<Void> future = new CompletableFuture<>();
        sendRequest(future, request);
        return future;
    }

    private static String deletionMethodSuffix(DeletionMethod method) {
        switch (method) {
            case DEPLETED:
                return "/deplete";
            case DISINTEGRATED:
                return "/poof";
        }
        throw new RuntimeException("Cannot occur");
    }

    public CompletableFuture<Void> deleteStar(StarKey key, DeletionMethod depleteOrPoof) {
        String url = config.httpUrl() + "/stars/" + key.getWorld() + "/" + key.getLocation() + deletionMethodSuffix(depleteOrPoof);

        Request request = new Request.Builder().url(url).delete().build();

        final CompletableFuture<Void> future = new CompletableFuture<>();
        sendRequest(future, request);
        return future;
    }

    public CompletableFuture<Void> callStar(long id) {
        String url = config.httpUrl() + "/stars/" + id + "/publish";

        RequestBody body = RequestBody.create(APPLICATION_JSON, GSON.toJson(true));
        Request request = new Request.Builder().url(url).method("PATCH", body).build();

        final CompletableFuture<Void> future = new CompletableFuture<>();
        sendRequest(future, request);
        return future;
    }


    private <T> void sendRequest(CompletableFuture<T> callback, Request request) {
        sendRequest(callback, request, null);
    }

    private <T> void sendRequest(CompletableFuture<T> callback, Request request, @Nullable ResponseBodyMapper<T> bodyMapper) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.completeExceptionally(new ResponseException(call, e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.completeExceptionally(new ResponseException(call, "WebServer answered with response code: " + response.code()));
                    return;
                }

                if (bodyMapper == null) {
                    callback.complete(null);
                } else {
                    ResponseBody body = response.body();
                    assert body != null;

                    callback.complete(bodyMapper.apply(body));
                }
            }
        });
    }

    private static interface ResponseBodyMapper<R> {
        public R apply(ResponseBody input) throws IOException;
    }

}

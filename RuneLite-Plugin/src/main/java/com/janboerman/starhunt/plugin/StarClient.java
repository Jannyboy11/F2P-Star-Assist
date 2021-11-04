package com.janboerman.starhunt.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.StarKey;
import com.janboerman.starhunt.common.StarLocation;
import com.janboerman.starhunt.common.StarTier;
import com.janboerman.starhunt.common.StarUpdate;
import com.janboerman.starhunt.common.web.EndPoints;
import com.janboerman.starhunt.common.web.StarJson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class StarClient {

    private static final MediaType APPLICATION_JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final StarHuntConfig config;

    @Inject
    public StarClient(OkHttpClient httpClient, StarHuntConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }


    public CompletableFuture<Set<CrashedStar>> requestStars() {
        String url = config.httpUrl() + EndPoints.ALL_STARS; //TODO + '/' + groupKey
        Request request = new Request.Builder().url(url).get().build();

        CompletableFuture<Set<CrashedStar>> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ResponseException(call, e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new ResponseException(call, "WebServer answered with response code: " + response.code()));
                        return;
                    }

                    ResponseBody body = response.body();
                    //assert content type json?
                    Reader reader = body.charStream();
                    JsonParser jsonParser = new JsonParser();

                    try {
                        JsonElement jsonElement = jsonParser.parse(reader);
                        if (jsonElement instanceof JsonArray) {
                            future.complete(StarJson.crashedStars((JsonArray) jsonElement));
                        } else {
                            future.completeExceptionally(new ResponseException(call, "Expected a json array of crashed stars, but got: " + jsonElement));
                        }
                    } catch (JsonParseException e) {
                        future.completeExceptionally(new ResponseException(call, e));
                    }
                } finally {
                    response.close();
                }
            }
        });

        return future;
    }

    public CompletableFuture<Optional<CrashedStar>> sendStar(CrashedStar star) {
        String url = config.httpUrl() /*TODO group key*/ + EndPoints.SEND_STAR;
        RequestBody requestBody = RequestBody.create(APPLICATION_JSON, StarJson.crashedStarJson(star).toString());
        Request request = new Request.Builder().url(url).put(requestBody).build();

        CompletableFuture<Optional<CrashedStar>> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ResponseException(call, e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new ResponseException(call, "WebServer answered with with response code: " + response.code()));
                    return;
                }

                switch (response.code()) {
                    case 200:
                        //assert content type json?
                        Reader reader = response.body().charStream();
                        JsonParser jsonParser = new JsonParser();

                        try {
                            JsonElement jsonElement = jsonParser.parse(reader);
                            if (jsonElement instanceof JsonObject) {
                                future.complete(Optional.of(StarJson.crashedStar((JsonObject) jsonElement)));
                            } else {
                                future.completeExceptionally(new ResponseException(call, "Expected a crashed star json object, but got: " + jsonElement));
                            }
                        } catch (JsonParseException e) {
                            future.completeExceptionally(new ResponseException(call, e));
                        }

                        break;
                    default:
                        future.complete(Optional.empty());
                        break;
                }
            }
        });

        return future;
    }

    public CompletableFuture<CrashedStar> updateStar(StarKey starKey, StarTier tier) {
        String url = config.httpUrl() /*TODO group key*/ + EndPoints.UPDATE_STAR;
        RequestBody requestBody = RequestBody.create(APPLICATION_JSON, StarJson.starUpdateJson(new StarUpdate(starKey, tier)).toString());
        Request request = new Request.Builder().url(url).patch(requestBody).build();

        CompletableFuture<CrashedStar> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ResponseException(call, e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new ResponseException(call, "WebServer answered with response code: " + response.code()));
                    return;
                }

                ResponseBody responseBody = response.body();
                //assert content type json?
                Reader reader = responseBody.charStream();
                JsonParser jsonParser = new JsonParser();

                try {
                    JsonElement jsonElement = jsonParser.parse(reader);
                    if (jsonElement instanceof JsonObject) {
                        future.complete(StarJson.crashedStar((JsonObject) jsonElement));
                    } else {
                        future.completeExceptionally(new ResponseException(call, "Expected a crashed star json object, but got: " + jsonElement));
                    }
                } catch (JsonParseException e) {
                    future.completeExceptionally(new ResponseException(call, e));
                }
            }
        });

        return future;
    }

    public CompletableFuture<Void> deleteStar(StarKey starKey) {
        String url = config.httpUrl() /*TODO group key*/ + EndPoints.DELETE_STAR;
        RequestBody requestBody = RequestBody.create(APPLICATION_JSON, StarJson.starKeyJson(starKey).toString());
        Request request = new Request.Builder().url(url).delete(requestBody).build();

        CompletableFuture<Void> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ResponseException(call, e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new ResponseException(call, "WebServer answered with response code: " + response.code()));
                    return;
                }

                future.complete(null);
            }
        });

        return future;
    }
}

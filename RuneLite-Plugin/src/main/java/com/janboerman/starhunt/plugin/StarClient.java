package com.janboerman.starhunt.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.StarLocation;
import com.janboerman.starhunt.common.StarTier;
import com.janboerman.starhunt.common.web.EndPoints;
import com.janboerman.starhunt.common.web.StarJson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class StarClient {

    private final OkHttpClient httpClient;
    private final StarHuntConfig config;

    @Inject
    public StarClient(OkHttpClient httpClient, StarHuntConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }


    public CompletableFuture<Set<CrashedStar>> requestStars() {
        String url = config.httpUrl() + EndPoints.ALL_STARS;
        Request request = new Request.Builder().url(url).build();

        CompletableFuture<Set<CrashedStar>> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        if (response.code() == 404) {
                            future.complete(null);
                        } else {
                            throw new IOException("Error retreiving data from StarHunter web server.");
                        }
                    }

                    ResponseBody body = response.body();
                    //assert content type json?
                    Reader reader = body.charStream();

                    JsonParser jsonParser = new JsonParser();
                    JsonElement jsonElement = jsonParser.parse(reader);

                    Set<CrashedStar> result = new HashSet<>();

                    if (jsonElement instanceof JsonArray) {
                        JsonArray jsonArray = (JsonArray) jsonElement;

                        for (JsonElement element : jsonArray) {
                            if (element instanceof JsonObject) {
                                JsonObject crashedStar = (JsonObject) element;
                                result.add(StarJson.crashedStar(crashedStar));
                            }
                        }
                    }

                    future.complete(result);

                } finally {
                    response.close();
                }
            }
        });

        return future;
    }
}

package com.janboerman.starhunt.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.StarKey;
import com.janboerman.starhunt.common.StarTier;
import com.janboerman.starhunt.common.StarUpdate;
import com.janboerman.starhunt.common.User;
import com.janboerman.starhunt.common.web.EndPoints;
import com.janboerman.starhunt.common.web.StarJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.time.Instant;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.Request;

class StarHandler extends AbstractHandler {

    private static final String APPLICATION_JSON = "application/json";

    private final StarDatabase starDatabase;

    StarHandler(StarDatabase starDatabase) {
        this.starDatabase = starDatabase;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        switch(target) {
            case EndPoints.ALL_STARS: receiveRequestStars(request, response); baseRequest.setHandled(true); break;
            case EndPoints.SEND_STAR: receiveSendStar(request, response); baseRequest.setHandled(true); break;
            case EndPoints.UPDATE_STAR: receiveUpdateStar(request, response); baseRequest.setHandled(true); break;
            case EndPoints.DELETE_STAR: receiveDeleteStar(request, response); baseRequest.setHandled(true); break;
            default: response.setStatus(HttpServletResponse.SC_NOT_FOUND); baseRequest.setHandled(true); break;
        }
    }

    private void receiveRequestStars(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch (request.getMethod()) {
            case "GET":
                response.setContentType(APPLICATION_JSON);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(StarJson.crashedStarsJson(starDatabase.getStars()).toString());
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    private void receiveSendStar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch (request.getMethod()) {
            case "PUT":
                try {
                    JsonElement jsonElement = JsonParser.parseReader(request.getReader());
                    if (jsonElement instanceof JsonObject jsonObject) {
                        CrashedStar star = StarJson.crashedStar(jsonObject);

                        //apply update
                        boolean isNew = starDatabase.add(star);

                        //response
                        if (isNew) {
                            //no need to reply with anything if the star is new
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        } else {
                            //reply with the currently existing star, if one already exists
                            star = starDatabase.get(star.getKey());

                            response.setContentType(APPLICATION_JSON);
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write(StarJson.crashedStarJson(star).toString());
                        }
                    }
                } catch (JsonParseException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    private void receiveUpdateStar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch(request.getMethod()) {
            case "PATCH":
                try {
                    JsonElement jsonElement = JsonParser.parseReader(request.getReader());
                    if (jsonElement instanceof JsonObject jsonObject) {
                        StarUpdate starUpdate = StarJson.starUpdate(jsonObject);
                        StarKey starKey = starUpdate.getKey();
                        StarTier newTier = starUpdate.getTier();
                        CrashedStar star = starDatabase.get(starKey);

                        //apply update
                        if (star != null) {
                            if (star.getTier().compareTo(newTier) > 0) {
                                star.setTier(newTier);
                            }
                        } else {
                            star = new CrashedStar(starKey, newTier, Instant.now(), User.unknown());
                            starDatabase.add(star);
                        }

                        //response
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType(APPLICATION_JSON);
                        response.getWriter().write(StarJson.crashedStarJson(star).toString());
                    } else {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                } catch (JsonParseException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    private void receiveDeleteStar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch (request.getMethod()) {
            case "DELETE":
                try {
                    JsonElement jsonElement = JsonParser.parseReader(request.getReader());
                    if (jsonElement instanceof JsonObject jsonObject) {
                        StarKey starKey = StarJson.starKey(jsonObject);

                        //apply update
                        starDatabase.remove(starKey);
                    }

                    //response
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);

                } catch (JsonParseException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

}

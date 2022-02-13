package com.janboerman.starhunt.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.GroupKey;
import com.janboerman.starhunt.common.StarKey;
import com.janboerman.starhunt.common.StarPacket;
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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.Request;

class StarHandler extends AbstractHandler {

    private static final String APPLICATION_JSON = "application/json";

    private final StarDatabase starDatabase;

    StarHandler(StarDatabase starDatabase) {
        this.starDatabase = starDatabase;
    }

    private static String extractKey(String target, String endpoint) {
        int endPointLength = endpoint.length();
        if (target.length() < endPointLength + 2) return null;
        if (target.charAt(endPointLength) != '/') return null;
        if (target.charAt(endPointLength + 1) == '/') return null;

        return target.substring(endPointLength + 1);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        //TODO rate limit

        match: {
            if (target.equals(EndPoints.ALL_STARS)) {
                String groupKey = extractKey(target, EndPoints.ALL_STARS);
                if (groupKey == null) break match;
                receiveRequestStars(request, response); baseRequest.setHandled(true); return;
            } else if (target.equals(EndPoints.SEND_STAR)) {
                String groupKey = extractKey(target, EndPoints.SEND_STAR);
                if (groupKey == null) break match;
                receiveSendStar(request, response); baseRequest.setHandled(true); return;
            } else if (target.equals(EndPoints.UPDATE_STAR)) {
                String groupKey = extractKey(target, EndPoints.UPDATE_STAR);
                if (groupKey == null) break match;
                receiveUpdateStar(request, response); baseRequest.setHandled(true); return;
            } else if (target.equals(EndPoints.DELETE_STAR)) {
                String groupKey = extractKey(target, EndPoints.DELETE_STAR);
                if (groupKey == null) break match;
                receiveDeleteStar(request, response); baseRequest.setHandled(true); return;
            }
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        baseRequest.setHandled(true);
    }

    private void receiveRequestStars(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch (request.getMethod()) {
            case "POST":
                //request body
                try {
                    JsonElement jsonElement = JsonParser.parseReader(request.getReader());
                    if (jsonElement instanceof JsonArray jsonArray) {
                        Set<GroupKey> groupKeys = StarJson.groupKeys(jsonArray);

                        //calculation
                        Set<CrashedStar> stars = new HashSet<>();
                        for (GroupKey groupKey : groupKeys) {
                            stars.addAll(starDatabase.getStars(groupKey));
                        }

                        //response
                        response.setContentType(APPLICATION_JSON);
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write(StarJson.crashedStarsJson(stars).toString());
                    }
                } catch (JsonParseException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
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
                        StarPacket starPacket = StarJson.starPacket(jsonObject);
                        Set<GroupKey> groups = starPacket.getGroups();
                        CrashedStar star = (CrashedStar) starPacket.getPayload();

                        //apply update
                        CrashedStar existing = null;
                        for (GroupKey groupKey : groups) {
                            CrashedStar ex = starDatabase.addIfAbsent(groupKey, star);
                            //find the 'smallest' existing star
                            if ((existing == null) || (ex != null && ex.getTier().getSize() < existing.getTier().getSize())) {
                                existing = ex;  //can still be null - that is fine.
                            }
                        }

                        //response
                        if (existing == null) {
                            //no need to reply with anything if the star is new.
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        } else {
                            //reply with the currently existing star, if one already exists.
                            star = existing;

                            response.setContentType(APPLICATION_JSON);
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write(StarJson.crashedStarJson(star).toString());
                        }
                    }
                } catch (JsonParseException | ClassCastException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    private void receiveUpdateStar(GroupKey groupKey, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //TODO group keys

        switch(request.getMethod()) {
            case "PATCH":
                try {
                    JsonElement jsonElement = JsonParser.parseReader(request.getReader());
                    if (jsonElement instanceof JsonObject jsonObject) {
                        StarUpdate starUpdate = StarJson.starUpdate(jsonObject);
                        StarKey starKey = starUpdate.getKey();
                        StarTier newTier = starUpdate.getTier();
                        CrashedStar star = starDatabase.get(groupKey, starKey);

                        //apply update
                        if (star != null) {
                            if (star.getTier().compareTo(newTier) > 0) {
                                //only update if the currently known tier is higher than the newly-received tier.
                                star.setTier(newTier);
                            }
                        } else {
                            //this shouldn't really happen.
                            star = new CrashedStar(starKey, newTier, Instant.now(), User.unknown());
                            starDatabase.add(groupKey, star);
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
        //TODO group keys

        switch (request.getMethod()) {
            case "DELETE":
                try {
                    JsonElement jsonElement = JsonParser.parseReader(request.getReader());
                    if (jsonElement instanceof JsonObject jsonObject) {
                        StarKey starKey = StarJson.starKey(jsonObject);

                        //apply update
                        starDatabase.remove(groupKey, starKey);
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

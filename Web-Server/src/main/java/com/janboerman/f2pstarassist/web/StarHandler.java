package com.janboerman.f2pstarassist.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.janboerman.f2pstarassist.common.CrashedStar;
import com.janboerman.f2pstarassist.common.GroupKey;
import com.janboerman.f2pstarassist.common.StarKey;
import com.janboerman.f2pstarassist.common.StarPacket;
import com.janboerman.f2pstarassist.common.StarTier;
import com.janboerman.f2pstarassist.common.StarUpdate;
import com.janboerman.f2pstarassist.common.User;
import com.janboerman.f2pstarassist.common.web.EndPoints;
import com.janboerman.f2pstarassist.common.web.StarJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;

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
        //TODO rate limit

        assert target != null;

        if (target.endsWith(EndPoints.ALL_STARS)) {
            receiveRequestStars(request, response);
            baseRequest.setHandled(true);
        } else if (target.endsWith(EndPoints.SEND_STAR)) {
            receiveSendStar(request, response);
            baseRequest.setHandled(true);
        } else if (target.endsWith(EndPoints.UPDATE_STAR)) {
            receiveUpdateStar(request, response);
            baseRequest.setHandled(true);
        } else if (target.endsWith(EndPoints.DELETE_STAR)) {
            receiveDeleteStar(request, response);
            baseRequest.setHandled(true);
        }

        else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            baseRequest.setHandled(true);
        }
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
                        Set<CrashedStar> stars = starDatabase.getStars(groupKeys);

                        //response
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType(APPLICATION_JSON);
                        response.getWriter().write(StarJson.crashedStarsJson(stars).toString());
                    }
                } catch (RuntimeException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
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
                        StarPacket starPacket = StarJson.starPacket(jsonObject);
                        Set<GroupKey> groups = starPacket.getGroups();
                        CrashedStar star = (CrashedStar) starPacket.getPayload();

                        //apply update
                        CrashedStar existing = starDatabase.add(groups, star);

                        //response
                        if (existing == null) {
                            //no need to reply with anything if the star is new.
                            response.setStatus(HttpServletResponse.SC_CREATED);
                        } else {
                            //reply with the currently existing star, if one already exists.
                            star = existing;

                            response.setContentType(APPLICATION_JSON);
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write(StarJson.crashedStarJson(star).toString());
                        }
                    }
                } catch (RuntimeException e) {
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
                        StarPacket starPacket = StarJson.starPacket(jsonObject);
                        Set<GroupKey> groups = starPacket.getGroups();
                        StarUpdate starUpdate = (StarUpdate) starPacket.getPayload();

                        StarKey starKey = starUpdate.getKey();
                        StarTier newTier = starUpdate.getTier();

                        CrashedStar resultStar;
                        Map<GroupKey, CrashedStar> knownStarsByGroup = starDatabase.update(groups, starUpdate);
                        if (!knownStarsByGroup.isEmpty()) {
                            //result is the earliest found star
                            resultStar = knownStarsByGroup.values().stream().reduce(BinaryOperator.minBy(Comparator.comparing(CrashedStar::getDetectedAt))).get();
                        } else {
                            //if the star was unknown to all of the groups
                            resultStar = new CrashedStar(starKey, newTier, Instant.now(), User.unknown());
                        }

                        //response
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType(APPLICATION_JSON);
                        response.getWriter().write(StarJson.crashedStarJson(resultStar).toString());
                    } else {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                } catch (RuntimeException e) {
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
                        StarPacket starPacket = StarJson.starPacket(jsonObject);
                        Set<GroupKey> groups = starPacket.getGroups();
                        StarKey starKey = (StarKey) starPacket.getPayload();

                        //apply update
                        for (GroupKey groupKey : groups) {
                            starDatabase.remove(groupKey, starKey);
                        }
                    }

                    //response
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);

                } catch (RuntimeException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

}

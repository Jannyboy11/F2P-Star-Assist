package com.janboerman.starhunt.common.web;

public class EndPoints {

    private EndPoints() {}

    public static final String ALL_STARS = "/stars";            //POST TODO is POST the most suitable request method?
    public static final String SEND_STAR = "/send_star";        //PUT
    public static final String UPDATE_STAR = "/update_star";    //PATCH
    public static final String DELETE_STAR = "/poofed_star";    //DELETE

}

/**
 * You can go to localhost:8080/greeting to test the api
 * It should say 403 with Spring Security working
 */

package com.bookolab.restservice.model;

public record Greeting(long id, String content) { }
// src/main/java/com/bookolab/restservice/dto/UserReactionStats.java
package com.bookolab.restservice.dto;

public class UserReactionStats {
    private long likesReceived;  // Renamed for clarity
    private long dislikesReceived;  // Renamed for clarity

    public UserReactionStats() {}

    public UserReactionStats(long likesReceived, long dislikesReceived) {
        this.likesReceived = likesReceived;
        this.dislikesReceived = dislikesReceived;
    }

    public long getLikesReceived() {
        return likesReceived;
    }

    public void setLikesReceived(long likesReceived) {
        this.likesReceived = likesReceived;
    }

    public long getDislikesReceived() {
        return dislikesReceived;
    }

    public void setDislikesReceived(long dislikesReceived) {
        this.dislikesReceived = dislikesReceived;
    }

    @Override
    public String toString() {
        return "UserReactionStats{likesReceived=" + likesReceived +
                ", dislikesReceived=" + dislikesReceived + "}";
    }
}
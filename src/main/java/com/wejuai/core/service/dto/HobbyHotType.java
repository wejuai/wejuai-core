package com.wejuai.core.service.dto;

public enum HobbyHotType {

    WATCHED(1),
    COMMENTED(1),
    CREATED(1),
    FOLLOWED(2);

    private final long point;

    HobbyHotType(long point) {
        this.point = point;
    }

    public long getPoint() {
        return point;
    }
}

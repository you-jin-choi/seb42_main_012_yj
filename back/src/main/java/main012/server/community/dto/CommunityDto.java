package main012.server.community.dto;

import lombok.*;

import java.util.List;

public class CommunityDto {

    @Getter
    @Setter
    public static class Post {
        private String title;
        private String Content;
        private Long tabId;
    }


    @Getter
    @Setter
    public static class Patch {
        private Long communityId;
        private String title;
        private String content;
//        private Long tabId;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long communityId;
        private String displayName;
        private String tabName;
        private String title;
        private String content;
        private long bookmarkCnt;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class listResponse<T> {
        private List<T> contents;
        private int totalElements;
        private Long nextCursor;
    }


}

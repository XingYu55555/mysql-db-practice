package com.sqljudge.resultservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private Long problemId;
    private String problemTitle;
    private List<LeaderboardEntry> entries;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}
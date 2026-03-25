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
public class OverallLeaderboardResponse {
    private List<OverallLeaderboardEntry> entries;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}
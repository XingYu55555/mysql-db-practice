package com.sqljudge.resultservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverallLeaderboardEntry {
    private Integer rank;
    private Long studentId;
    private String studentUsername;
    private BigDecimal totalScore;
    private Integer problemsSolved;
    private Integer totalSubmissions;
}
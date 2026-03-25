package com.sqljudge.resultservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private Integer rank;
    private Long studentId;
    private String studentUsername;
    private BigDecimal bestScore;
    private LocalDateTime latestSubmitTime;
}
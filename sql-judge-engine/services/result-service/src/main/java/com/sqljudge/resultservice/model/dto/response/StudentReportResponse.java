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
public class StudentReportResponse {
    private Long studentId;
    private Integer totalSubmissions;
    private Integer correctSubmissions;
    private Double averageScore;
    private List<SubmissionDetailDTO> submissionDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionDetailDTO {
        private Long submissionId;
        private Long problemId;
        private String problemTitle;
        private Integer score;
        private String status;
        private Long executionTimeMs;
        private String submittedAt;
    }
}
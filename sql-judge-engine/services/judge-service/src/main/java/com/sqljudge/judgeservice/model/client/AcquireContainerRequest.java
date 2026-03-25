package com.sqljudge.judgeservice.model.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquireContainerRequest {
    private Long problemId;
    private Integer timeout;
}

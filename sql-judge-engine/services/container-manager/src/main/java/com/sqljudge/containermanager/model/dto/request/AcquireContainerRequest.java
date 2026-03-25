package com.sqljudge.containermanager.model.dto.request;

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

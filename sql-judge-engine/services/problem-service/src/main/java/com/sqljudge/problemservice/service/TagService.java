package com.sqljudge.problemservice.service;

import com.sqljudge.problemservice.model.dto.request.CreateTagRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemTagsRequest;
import com.sqljudge.problemservice.model.dto.response.TagListResponse;
import com.sqljudge.problemservice.model.dto.response.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> listTags();

    TagResponse createTag(CreateTagRequest request);

    TagListResponse updateProblemTags(Long problemId, UpdateProblemTagsRequest request);
}
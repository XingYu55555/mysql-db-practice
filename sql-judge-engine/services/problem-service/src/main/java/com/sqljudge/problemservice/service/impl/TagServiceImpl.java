package com.sqljudge.problemservice.service.impl;

import com.sqljudge.problemservice.exception.ProblemNotFoundException;
import com.sqljudge.problemservice.exception.TagNotFoundException;
import com.sqljudge.problemservice.model.dto.request.CreateTagRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemTagsRequest;
import com.sqljudge.problemservice.model.dto.response.TagListResponse;
import com.sqljudge.problemservice.model.dto.response.TagResponse;
import com.sqljudge.problemservice.model.entity.ProblemTag;
import com.sqljudge.problemservice.model.entity.Tag;
import com.sqljudge.problemservice.repository.ProblemRepository;
import com.sqljudge.problemservice.repository.ProblemTagRepository;
import com.sqljudge.problemservice.repository.TagRepository;
import com.sqljudge.problemservice.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final ProblemTagRepository problemTagRepository;
    private final ProblemRepository problemRepository;

    @Override
    public List<TagResponse> listTags() {
        return tagRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        Tag tag = Tag.builder()
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#3B82F6")
                .build();

        Tag saved = tagRepository.save(tag);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TagListResponse updateProblemTags(Long problemId, UpdateProblemTagsRequest request) {
        if (!problemRepository.existsById(problemId)) {
            throw new ProblemNotFoundException(problemId);
        }

        problemTagRepository.deleteByProblemId(problemId);

        for (Long tagId : request.getTagIds()) {
            if (!tagRepository.existsById(tagId)) {
                throw new TagNotFoundException(tagId);
            }

            ProblemTag problemTag = ProblemTag.builder()
                    .problemId(problemId)
                    .tagId(tagId)
                    .build();
            problemTagRepository.save(problemTag);
        }

        List<TagResponse> tags = tagRepository.findAllById(request.getTagIds()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return TagListResponse.builder()
                .tags(tags)
                .build();
    }

    private TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .tagId(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
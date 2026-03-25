package com.sqljudge.problemservice.controller;

import com.sqljudge.problemservice.model.dto.request.CreateTagRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemTagsRequest;
import com.sqljudge.problemservice.model.dto.response.TagListResponse;
import com.sqljudge.problemservice.model.dto.response.TagResponse;
import com.sqljudge.problemservice.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tag", description = "Tag management endpoints")
public class TagController {

    private final TagService tagService;

    @GetMapping("/api/tag")
    @Operation(summary = "List all tags")
    public ResponseEntity<List<TagResponse>> listTags() {
        List<TagResponse> response = tagService.listTags();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/tag")
    @Operation(summary = "Create a new tag")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        TagResponse response = tagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/problem/{problemId}/tags")
    @Operation(summary = "Update problem tags")
    public ResponseEntity<TagListResponse> updateProblemTags(
            @PathVariable Long problemId,
            @Valid @RequestBody UpdateProblemTagsRequest request) {
        TagListResponse response = tagService.updateProblemTags(problemId, request);
        return ResponseEntity.ok(response);
    }
}
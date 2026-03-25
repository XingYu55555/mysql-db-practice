package com.sqljudge.problemservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problemservice.exception.ProblemNotFoundException;
import com.sqljudge.problemservice.exception.TagNotFoundException;
import com.sqljudge.problemservice.model.dto.request.CreateTagRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemTagsRequest;
import com.sqljudge.problemservice.model.dto.response.TagListResponse;
import com.sqljudge.problemservice.model.dto.response.TagResponse;
import com.sqljudge.problemservice.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TagService tagService;

    @Test
    void listTags_Success() throws Exception {
        TagResponse tag = TagResponse.builder()
                .tagId(1L)
                .name("TestTag")
                .color("#3B82F6")
                .createdAt(LocalDateTime.now())
                .build();

        when(tagService.listTags()).thenReturn(List.of(tag));

        mockMvc.perform(get("/api/tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tagId").value(1))
                .andExpect(jsonPath("$[0].name").value("TestTag"));
    }

    @Test
    void createTag_Success() throws Exception {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("NewTag")
                .color("#FF0000")
                .build();

        TagResponse response = TagResponse.builder()
                .tagId(1L)
                .name("NewTag")
                .color("#FF0000")
                .createdAt(LocalDateTime.now())
                .build();

        when(tagService.createTag(any())).thenReturn(response);

        mockMvc.perform(post("/api/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tagId").value(1))
                .andExpect(jsonPath("$.name").value("NewTag"))
                .andExpect(jsonPath("$.color").value("#FF0000"));
    }

    @Test
    void createTag_ValidationError() throws Exception {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProblemTags_Success() throws Exception {
        UpdateProblemTagsRequest request = UpdateProblemTagsRequest.builder()
                .tagIds(List.of(1L, 2L))
                .build();

        TagListResponse response = TagListResponse.builder()
                .tags(List.of(
                        TagResponse.builder().tagId(1L).name("Tag1").color("#3B82F6").build(),
                        TagResponse.builder().tagId(2L).name("Tag2").color("#FF0000").build()
                ))
                .build();

        when(tagService.updateProblemTags(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/problem/1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void updateProblemTags_ProblemNotFound() throws Exception {
        UpdateProblemTagsRequest request = UpdateProblemTagsRequest.builder()
                .tagIds(List.of(1L))
                .build();

        when(tagService.updateProblemTags(eq(999L), any()))
                .thenThrow(new ProblemNotFoundException(999L));

        mockMvc.perform(put("/api/problem/999/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProblemTags_TagNotFound() throws Exception {
        UpdateProblemTagsRequest request = UpdateProblemTagsRequest.builder()
                .tagIds(List.of(999L))
                .build();

        when(tagService.updateProblemTags(eq(1L), any()))
                .thenThrow(new TagNotFoundException(999L));

        mockMvc.perform(put("/api/problem/1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
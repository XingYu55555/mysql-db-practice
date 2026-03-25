package com.sqljudge.problemservice.service;

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
import com.sqljudge.problemservice.service.impl.TagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ProblemTagRepository problemTagRepository;

    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    private Tag testTag;

    @BeforeEach
    void setUp() {
        testTag = Tag.builder()
                .id(1L)
                .name("TestTag")
                .color("#3B82F6")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void listTags_Success() {
        when(tagRepository.findAll()).thenReturn(List.of(testTag));

        List<TagResponse> response = tagService.listTags();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("TestTag", response.get(0).getName());
    }

    @Test
    void listTags_Empty() {
        when(tagRepository.findAll()).thenReturn(Collections.emptyList());

        List<TagResponse> response = tagService.listTags();

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void createTag_Success() {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("NewTag")
                .color("#FF0000")
                .build();

        Tag savedTag = Tag.builder()
                .id(1L)
                .name("NewTag")
                .color("#FF0000")
                .createdAt(LocalDateTime.now())
                .build();

        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

        TagResponse response = tagService.createTag(request);

        assertNotNull(response);
        assertEquals("NewTag", response.getName());
        assertEquals("#FF0000", response.getColor());
    }

    @Test
    void createTag_WithDefaultColor() {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("NewTag")
                .build();

        Tag savedTag = Tag.builder()
                .id(1L)
                .name("NewTag")
                .color("#3B82F6")
                .createdAt(LocalDateTime.now())
                .build();

        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

        TagResponse response = tagService.createTag(request);

        assertNotNull(response);
        assertEquals("#3B82F6", response.getColor());
    }

    @Test
    void updateProblemTags_Success() {
        UpdateProblemTagsRequest request = UpdateProblemTagsRequest.builder()
                .tagIds(List.of(1L, 2L))
                .build();

        Tag tag1 = Tag.builder()
                .id(1L)
                .name("Tag1")
                .color("#3B82F6")
                .createdAt(LocalDateTime.now())
                .build();

        Tag tag2 = Tag.builder()
                .id(2L)
                .name("Tag2")
                .color("#FF0000")
                .createdAt(LocalDateTime.now())
                .build();

        when(problemRepository.existsById(1L)).thenReturn(true);
        doNothing().when(problemTagRepository).deleteByProblemId(1L);
        when(tagRepository.existsById(1L)).thenReturn(true);
        when(tagRepository.existsById(2L)).thenReturn(true);
        when(problemTagRepository.save(any(ProblemTag.class))).thenReturn(new ProblemTag());
        when(tagRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(tag1, tag2));

        TagListResponse response = tagService.updateProblemTags(1L, request);

        assertNotNull(response);
        assertEquals(2, response.getTags().size());
    }

    @Test
    void updateProblemTags_ProblemNotFound() {
        UpdateProblemTagsRequest request = UpdateProblemTagsRequest.builder()
                .tagIds(List.of(1L))
                .build();

        when(problemRepository.existsById(999L)).thenReturn(false);

        assertThrows(ProblemNotFoundException.class,
                () -> tagService.updateProblemTags(999L, request));
    }

    @Test
    void updateProblemTags_TagNotFound() {
        UpdateProblemTagsRequest request = UpdateProblemTagsRequest.builder()
                .tagIds(List.of(999L))
                .build();

        when(problemRepository.existsById(1L)).thenReturn(true);
        doNothing().when(problemTagRepository).deleteByProblemId(1L);
        when(tagRepository.existsById(999L)).thenReturn(false);

        assertThrows(TagNotFoundException.class,
                () -> tagService.updateProblemTags(1L, request));
    }
}
package com.nm.userservice.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.userservice.Dto.UserResponseDto;
import com.nm.userservice.Dto.UserUpdateRequestDto;
import com.nm.userservice.Entity.UserProfile;
import com.nm.userservice.Repository.UserOnboardingProgressRepository;
import com.nm.userservice.Repository.UserRepository;
import com.nm.userservice.Repository.UserRoutinePreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(
                userRepository,
                mock(UserOnboardingProgressRepository.class),
                mock(UserRoutinePreferenceRepository.class),
                new ObjectMapper(),
                mock(AiImageAnalysisService.class)
        );
    }

    @Test
    void emptyProfileReturnsExplicitFrontendSignals() {
        UserProfile profile = UserProfile.builder()
                .userId(1L)
                .onboardingStatus("INCOMPLETE")
                .build();
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UserResponseDto response = userService.getProfile(1L);

        assertTrue(response.isProfileEmpty());
        assertFalse(response.isProfileCompleted());
        assertEquals("INCOMPLETE", response.getOnboardingStatus());
    }

    @Test
    void blankProfileUpdateIsRejected() {
        UserProfile profile = UserProfile.builder().userId(1L).nickname("기존닉네임").build();
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setNickname("   \t");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateProfile(1L, request)
        );

        assertEquals("nickname은(는) 공백으로 저장할 수 없습니다.", exception.getMessage());
        assertEquals("기존닉네임", profile.getNickname());
        verify(userRepository, never()).save(any());
    }

    @Test
    void profileUpdateTrimsLeadingAndTrailingWhitespace() {
        UserProfile profile = UserProfile.builder().userId(1L).build();
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setNickname("  새 닉네임  ");

        userService.updateProfile(1L, request);

        assertEquals("새 닉네임", profile.getNickname());
        verify(userRepository).save(profile);
    }
}

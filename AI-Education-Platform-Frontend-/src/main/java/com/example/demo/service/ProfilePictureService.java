package com.example.demo.service;

import com.example.demo.model.ProfilePicture;
import com.example.demo.repository.ProfilePictureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;

@Service
public class ProfilePictureService {

    @Autowired
    private ProfilePictureRepository profilePictureRepository;

    @Transactional
    public ProfilePicture saveProfilePicture(Long userId, MultipartFile file) throws IOException {
        // Delete existing profile picture if any
        profilePictureRepository.deleteByUserId(userId);

        // Create new profile picture
        ProfilePicture profilePicture = new ProfilePicture(
            userId,
            file.getBytes(),
            "profile", // picture type
            file.getContentType(),
            file.getOriginalFilename()
        );

        return profilePictureRepository.save(profilePicture);
    }

    public Optional<ProfilePicture> getProfilePicture(Long userId) {
        return profilePictureRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteProfilePicture(Long userId) {
        profilePictureRepository.deleteByUserId(userId);
    }
}

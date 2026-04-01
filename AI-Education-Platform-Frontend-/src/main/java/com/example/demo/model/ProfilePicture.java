package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pfps")
public class ProfilePicture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Lob
    @Column(name = "picture_data", columnDefinition = "LONGBLOB")
    private byte[] pictureData;

    @Column(name = "picture_type", length = 50)
    private String pictureType;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    // Constructors
    public ProfilePicture() {}

    public ProfilePicture(Long userId) {
        this.userId = userId;
        this.uploadedAt = LocalDateTime.now();
    }

    public ProfilePicture(Long userId, byte[] pictureData, String pictureType, String contentType, String fileName) {
        this.userId = userId;
        this.pictureData = pictureData;
        this.pictureType = pictureType;
        this.contentType = contentType;
        this.fileName = fileName;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public byte[] getPictureData() {
        return pictureData;
    }

    public void setPictureData(byte[] pictureData) {
        this.pictureData = pictureData;
        this.uploadedAt = LocalDateTime.now();
    }

    public String getPictureType() {
        return pictureType;
    }

    public void setPictureType(String pictureType) {
        this.pictureType = pictureType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}

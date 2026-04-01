package com.example.demo.validation;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class FileSizeValidator implements ConstraintValidator<FileSize, MultipartFile> {
    private long maxSizeInMB;

    @Override
    public void initialize(FileSize constraintAnnotation) {
        this.maxSizeInMB = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        // If file is null or empty, consider it valid (use @NotNull separately if required)
        if (file == null || file.isEmpty()) {
            return true;
        }

        // Convert MB to bytes
        long maxSizeInBytes = maxSizeInMB * 1024 * 1024;
        
        // Check file size
        return file.getSize() <= maxSizeInBytes;
    }
}

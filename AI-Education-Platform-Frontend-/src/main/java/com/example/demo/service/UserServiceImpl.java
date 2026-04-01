package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.web.dto.UserRegistrationDto;
import com.example.demo.web.dto.UserEditProfileDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Map to store OTPs with associated emails
    private final Map<String, String> otpMap = new HashMap<>();

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User save(UserRegistrationDto registrationDto) {
        System.out.println("Attempting to save user with email: " + registrationDto.getEmail());

        User existingUser = userRepository.findByEmail(registrationDto.getEmail());
        if (existingUser != null) {
            System.out.println("Email already exists: " + registrationDto.getEmail());
            throw new IllegalArgumentException("Email already exists.");
        }

        User user = new User(
            registrationDto.getFirstName(),
            registrationDto.getLastName(),
            registrationDto.getEmail(),
            passwordEncoder.encode(registrationDto.getPassword()),
            Arrays.asList(new Role("ROLE_USER"))
        );

        User savedUser = userRepository.save(user);
        System.out.println("User saved successfully: " + savedUser.getEmail());
        return savedUser;
    }

    @Override
    public User findByEmail(String email) {
        System.out.println("Finding user by email: " + email);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            System.out.println("No user found with email: " + email);
        } else {
            System.out.println("User found: " + user.getEmail());
        }
        return user;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByEmail(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Loading user by username: " + username);
        User user = userRepository.findByEmail(username);
        if (user == null) {
            System.out.println("Invalid username or password for: " + username);
            throw new UsernameNotFoundException("Invalid username or password.");
        }
        System.out.println("User loaded: " + user.getEmail());
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), mapRolesToAuthorities(user.getRoles()));
    }

    @Override
    public String generateOtp(String email) {
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        storeOtp(email, otp);
        return otp;
    }

    // Method to store OTP for an email
    public void storeOtp(String email, String otp) {
        otpMap.put(email, otp);
        System.out.println("Stored OTP: " + otp + " for email: " + email);
    }

    // Method to verify the OTP for an email
    public boolean verifyOtp(String email, String otp) {
        System.out.println("Verifying OTP for email: " + email);
        String storedOtp = otpMap.get(email);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpMap.remove(email); // Remove the OTP after successful verification
            System.out.println("OTP verified successfully for email: " + email);
            return true;
        }
        System.out.println("Invalid OTP for email: " + email);
        return false;
    }

    // Mapping roles to authorities
    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toList());
    }
    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public void updateUser(User user) {
        userRepository.save(user);
    }
    @Override
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public User editProfile(Long userId, UserEditProfileDto editProfileDto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Update basic profile information
        user.setFirstName(editProfileDto.getFirstName());
        user.setLastName(editProfileDto.getLastName());
        user.setPhoneNumber(editProfileDto.getPhoneNumber());
        user.setDateOfBirth(editProfileDto.getDateOfBirth());
        user.setSex(editProfileDto.getSex());
        user.setCountry(editProfileDto.getCountry());
        user.setAddress(editProfileDto.getAddress());

        return userRepository.save(user);
    }

    // Helper method to resize and crop image
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        // Calculate aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double aspectRatio = (double) originalWidth / originalHeight;

        // Determine new dimensions while maintaining aspect ratio
        int newWidth, newHeight;
        if (aspectRatio > 1) {
            // Wide image
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * aspectRatio);
        } else {
            // Tall image
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / aspectRatio);
        }

        // Resize image
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // Calculate crop offsets
        int x = (newWidth - targetWidth) / 2;
        int y = (newHeight - targetHeight) / 2;
        
        g2d.drawImage(originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 
                      -x, -y, null);
        g2d.dispose();

        return resizedImage;
    }
}

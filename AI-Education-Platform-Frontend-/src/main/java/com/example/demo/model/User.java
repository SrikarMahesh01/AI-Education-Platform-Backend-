package com.example.demo.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.*;
import java.time.LocalDate;
@Entity
@Table(name = "user", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String email;
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "survey_completed", columnDefinition = "integer default 0")
    private int surveyCompleted;

    @Column(name = "quiz_completed", columnDefinition = "integer default 0")
    private int quizCompleted;

    @Column(name = "course_selected_1")
    private String courseSelected1;

    @Column(name = "course_selected_2")
    private String courseSelected2;

    @Column(name = "course_selected_3")
    private String courseSelected3;

    @Column(name = "course1_progress", columnDefinition = "integer default 0")
    private Integer course1Progress = 0;

    @Column(name = "course2_progress", columnDefinition = "integer default 0")
    private Integer course2Progress = 0;

    @Column(name = "course3_progress", columnDefinition = "integer default 0")
    private Integer course3Progress = 0;

    @Column(name = "sex")
    private String sex;

    @Column(name = "country")
    private String country;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
        name = "users_roles",
        joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Collection<Role> roles;

    @ElementCollection
    @CollectionTable(name = "user_activity_progress", 
                    joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "activity_key")
    @Column(name = "completed")
    private Map<String, Boolean> activityProgress = new HashMap<>();

    // Constructors
    public User() {}

    public User(String firstName, String lastName, String email, String password, Collection<Role> roles, String sex) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.sex = sex;
        this.surveyCompleted = 0;
        this.quizCompleted = 0;
    }

    public User(String firstName, String lastName, String email, String password, Collection<Role> roles) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.roles = roles;
        
        // Set default values for new fields
        this.surveyCompleted = 0;
        this.quizCompleted = 0;
        this.courseSelected1 = null;
        this.courseSelected2 = null;
        this.courseSelected3 = null;
        this.sex = null;
        this.phoneNumber = null;
        this.dateOfBirth = null;
        this.country = null;
        this.address = null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getSurveyCompleted() {
        return surveyCompleted;
    }

    public void setSurveyCompleted(Integer surveyCompleted) {
        this.surveyCompleted = surveyCompleted;
    }

    public Integer getQuizCompleted() {
        return quizCompleted;
    }

    public void setQuizCompleted(Integer quizCompleted) {
        this.quizCompleted = quizCompleted;
    }

    public String getCourseSelected1() {
        return courseSelected1;
    }

    public void setCourseSelected1(String courseSelected1) {
        this.courseSelected1 = courseSelected1;
    }

    public String getCourseSelected2() {
        return courseSelected2;
    }

    public void setCourseSelected2(String courseSelected2) {
        this.courseSelected2 = courseSelected2;
    }

    public String getCourseSelected3() {
        return courseSelected3;
    }

    public void setCourseSelected3(String courseSelected3) {
        this.courseSelected3 = courseSelected3;
    }

    public Integer getCourse1Progress() {
        return course1Progress;
    }

    public void setCourse1Progress(Integer course1Progress) {
        this.course1Progress = course1Progress;
    }

    public Integer getCourse2Progress() {
        return course2Progress;
    }

    public void setCourse2Progress(Integer course2Progress) {
        this.course2Progress = course2Progress;
    }

    public Integer getCourse3Progress() {
        return course3Progress;
    }

    public void setCourse3Progress(Integer course3Progress) {
        this.course3Progress = course3Progress;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Collection<Role> getRoles() {
        return roles;
    }

    public void setRoles(Collection<Role> roles) {
        this.roles = roles;
    }

    public Map<String, Boolean> getActivityProgress() {
        return activityProgress != null ? activityProgress : new HashMap<>();
    }

    public void setActivityProgress(Map<String, Boolean> activityProgress) {
        this.activityProgress = activityProgress;
    }

    public void markActivityCompleted(String courseName, int moduleIndex, String activity) {
        String progressKey = courseName + "_" + moduleIndex + "_" + activity;
        this.activityProgress.put(progressKey, true);
    }

    public void markActivityIncomplete(String courseName, int moduleIndex, String activity) {
        String progressKey = courseName + "_" + moduleIndex + "_" + activity;
        this.activityProgress.put(progressKey, false);
    }

    public boolean isActivityCompleted(String courseName, int moduleIndex, String activity) {
        String progressKey = courseName + "_" + moduleIndex + "_" + activity;
        return Boolean.TRUE.equals(this.activityProgress.get(progressKey));
    }
}

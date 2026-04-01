package com.example.demo.model;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LearningPath {
    @JsonProperty("learning_path")
    private PathContent content;

    public LearningPath() {
        this.content = new PathContent();
    }

    public static class PathContent {
        private List<Module> modules = new ArrayList<>();
        private List<String> prerequisites = new ArrayList<>();
        @JsonProperty("quiz_adaptations")
        private Object quizAdaptations;

        public List<Module> getModules() {
            return modules;
        }

        public void setModules(List<Module> modules) {
            this.modules = modules != null ? modules : new ArrayList<>();
        }

        public List<String> getPrerequisites() {
            return prerequisites;
        }

        public void setPrerequisites(List<String> prerequisites) {
            this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
        }

        public Object getQuizAdaptations() {
            return quizAdaptations;
        }

        public void setQuizAdaptations(Object quizAdaptations) {
            this.quizAdaptations = quizAdaptations;
        }
    }

    public List<Module> getModules() {
        return content != null ? content.getModules() : new ArrayList<>();
    }

    public List<String> getPrerequisites() {
        return content != null ? content.getPrerequisites() : new ArrayList<>();
    }

    public PathContent getContent() {
        return content;
    }

    public void setContent(PathContent content) {
        this.content = content != null ? content : new PathContent();
    }
}

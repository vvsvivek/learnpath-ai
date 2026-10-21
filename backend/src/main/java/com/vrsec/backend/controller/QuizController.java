package com.vrsec.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vrsec.backend.service.QuizGenerationService;

@RestController
@RequestMapping("/api/main/quiz")
@CrossOrigin(origins = "*") 
public class QuizController {

    @Autowired
    private QuizGenerationService quizService;

   @GetMapping("/generate-dynamic")
    public ResponseEntity<?> generateDynamicQuiz(
            @RequestParam String topic, 
            @RequestParam String course,
            @RequestParam(required = false) String _t) {
        
        String jsonQuiz = quizService.generateUniqueQuestions(topic, course);
        return ResponseEntity.ok().body(jsonQuiz);
    }
}
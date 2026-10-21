package com.vrsec.backend.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private ChatSession session;
    
    private String sender;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private Date timestamp = new Date();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ChatSession getSession() { return session; }
    public void setSession(ChatSession session) { this.session = session; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
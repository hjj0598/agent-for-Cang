package com.example.demo.service;

import com.example.demo.dto.RelationshipReminder;

import java.util.List;

public interface RelationshipReminderService {

    List<RelationshipReminder> list(Long userId);
}

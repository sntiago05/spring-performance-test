package com.helpflow.supportticket.application.model;

import com.helpflow.supportticket.domain.model.Category;

public record TechnicianSummary(Long id, String code, String fullName, Category specialty) {
}

package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlankpublic class DependencyRequestDto {
    @NotBlank
    private String stepStrId;

    @NotBlank
    private String prerequisiteStepStrId;

    
}

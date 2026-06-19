package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeGenerateResponse {
    private int year;
    private int month;
    private int created;
    private int skipped;
}

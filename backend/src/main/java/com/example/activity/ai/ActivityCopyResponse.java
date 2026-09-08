package com.example.activity.ai;

import java.util.List;

public record ActivityCopyResponse(
        String title,
        String description,
        List<String> highlights,
        List<String> notices,
        List<String> tags,
        String model
) {
}

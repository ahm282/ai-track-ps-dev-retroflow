package com.stackcraft.retroflow.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({RenameMemberRequest.class, NameFormat.class})
public record RenameMemberRequest(
    @NotBlank(message = "Member name must not be blank")
    @Size(max = 255, message = "Member name must be at most 255 characters")
    @Pattern(regexp = "^ *[A-Za-z0-9]+( +[A-Za-z0-9]+)* *$", groups = NameFormat.class,
        message = "Member name must contain only letters, digits and spaces")
    String name
) {
}

package com.wakilfly.dto.request;

import com.wakilfly.model.ReportReason;
import com.wakilfly.model.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    @NotNull(message = "Report type is required")
    private ReportType type;

    @NotNull(message = "Target ID is required")
    private UUID targetId;

    @NotNull(message = "Reason is required")
    private ReportReason reason;

    private String description;
}

package com.viajajunto.api.modules.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapMarkerDTO {
    /** "destination" ou "activity", conforme o contrato OpenAPI. */
    private String type;
    private String id;
    private String label;
    private Double latitude;
    private Double longitude;
}

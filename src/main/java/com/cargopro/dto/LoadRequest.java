package com.cargopro.dto;

import com.cargopro.enums.WeightUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LoadRequest {

    @NotBlank(message = "Shipper ID is required")
    private String shipperId;

    @NotBlank(message = "Loading city is required")
    private String loadingCity;

    @NotBlank(message = "Unloading city is required")
    private String unloadingCity;

    @NotBlank(message = "Product type is required")
    private String productType;

    @NotBlank(message = "Truck type is required")
    private String truckType;

    @NotNull(message = "Number of trucks is required")
    @Min(value = 1, message = "Number of trucks must be at least 1")
    private Integer noOfTrucks;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private Double weight;

    @NotNull(message = "Weight unit is required")
    private WeightUnit weightUnit;

    private String comment;
}

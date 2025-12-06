package com.cargopro.dto;

import com.cargopro.enums.LoadStatus;
import com.cargopro.enums.WeightUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
public class LoadResponse {
    private UUID loadId;
    private String shipperId;
    private String loadingCity;
    private String unloadingCity;
    private Timestamp loadingDate;
    private String productType;
    private Double weight;
    private WeightUnit weightUnit;
    private String truckType;
    private Integer noOfTrucks;
    private Integer remainingTrucks;
    private LoadStatus status;
    private Timestamp datePosted;
}

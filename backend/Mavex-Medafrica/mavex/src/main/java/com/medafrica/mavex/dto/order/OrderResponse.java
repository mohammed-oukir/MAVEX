package com.medafrica.mavex.dto.order;

import com.medafrica.mavex.model.enums.OrderStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {

    private Long id;
    private String hawb;
    private String goodsDescription;
    private String htsusCode;
    private Integer numberOfItems;
    private BigDecimal shipmentWeight;
    private BigDecimal grossWeight;
    private BigDecimal netQuantity;
    private Integer manifestQty;
    private BigDecimal customsValue;
    private String customsCurrency;
    private BigDecimal dutyRate;
    private BigDecimal dutyAmount;
    private BigDecimal bankCharges;
    private BigDecimal totalAmount;
    private BigDecimal enteredValue;
    private OrderStatus status;

    /** Token exposé uniquement si présent et valide */
    private String paymentToken;
    private LocalDateTime tokenExpiresAt;
    private boolean tokenValid;

    // Relations résumées
    private Long shipmentId;
    private String mawb;          // Shipment.mawb pour affichage
    private Long clientId;
    private String clientFullName; // Client.firstName + lastName


    // après clientFullName////nv ajout
private String clientEmail;
private String clientPhone;
private String clientAddress;
private String clientCity;
private String clientState;
private String clientZipCode;
private String clientCountry;
 private String companyName;

    private LocalDateTime emailSentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
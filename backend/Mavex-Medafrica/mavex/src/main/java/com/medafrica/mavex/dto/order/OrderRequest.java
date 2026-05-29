package com.medafrica.mavex.dto.order;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderRequest {

    @NotBlank(message = "HAWB est obligatoire")
    private String hawb;

    private String goodsDescription;

    private String htsusCode;

    @Min(value = 1, message = "Le nombre d'articles doit être >= 1")
    private Integer numberOfItems;

    @NotNull(message = "Le poids est obligatoire")
    @DecimalMin(value = "0.001", message = "Le poids doit être positif")
    private BigDecimal shipmentWeight;

    private BigDecimal grossWeight;

    private BigDecimal netQuantity;

    private Integer manifestQty;

    @NotNull(message = "La valeur douanière est obligatoire")
    @DecimalMin(value = "0.00", inclusive = false, message = "La valeur douanière doit être positive")
    private BigDecimal customsValue;

    @Size(min = 3, max = 3, message = "Le code devise doit être sur 3 caractères (ex: USD)")
    private String customsCurrency;

    @DecimalMin(value = "0.0000", message = "Le taux douanier doit être positif")
    @DecimalMax(value = "1.0000", message = "Le taux douanier ne peut pas dépasser 100%")
    private BigDecimal dutyRate;

    private BigDecimal bankCharges;

    private BigDecimal enteredValue;

    /** FK -> shipments.id */
    @NotNull(message = "Le shipment parent est obligatoire")
    private Long shipmentId;

    /** FK -> clients.id */
    @NotNull(message = "Le client destinataire est obligatoire")
    private Long clientId;

    
}
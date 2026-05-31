package com.medafrica.mavex.dto.imports;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ImportConfirmRequest {

    private String fileName;
    private String fileHash;
    private List<RowData> rows;

    @Data
    public static class RowData {
        private int    rowNumber;
        private String mawb;
        private String hawb;
        private String alternateReference;
        private String senderName;
        private String senderCountry;
        private String senderAddress;
        private String senderCity;
        private String senderState;
        private String senderPostcode;
        private String senderContact;
        private String senderPhone;
        private String senderEmail;
        private String receiverName;
        private String receiverCountry;
        private String receiverAddress;
        private String receiverCity;
        private String receiverState;
        private String receiverPostcode;
        private String receiverContact;
        private String receiverPhone;
        private String receiverEmail;
        private Integer    numberOfItems;
        private String     goodsDescription;
        private BigDecimal shipmentWeight;
        private BigDecimal customsValue;
        private String     customsCurrency;
    }
}

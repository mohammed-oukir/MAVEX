package com.medafrica.mavex.service.imports;

import java.util.Set;

/**
 * Indices logiques des colonnes du manifeste Excel + en-têtes attendus.
 * Partagé entre {@code ExcelImportServiceImpl} (lecture/validation) et
 * {@code ImportRowProcessor} (création des entités).
 */
public final class ImportColumns {

    private ImportColumns() {}

    public static final int COL_MAWB               = 0;
    public static final int COL_HAWB               = 1;
    public static final int COL_ALTERNATE_REF      = 2;
    public static final int COL_SENDER_NAME        = 3;
    public static final int COL_SENDER_COUNTRY     = 4;
    public static final int COL_SENDER_ADDRESS     = 5;
    public static final int COL_SENDER_CITY        = 6;
    public static final int COL_SENDER_STATE       = 7;
    public static final int COL_SENDER_POSTCODE    = 8;
    public static final int COL_SENDER_CONTACT     = 9;
    public static final int COL_SENDER_PHONE       = 10;
    public static final int COL_SENDER_EMAIL       = 11;
    public static final int COL_RECEIVER_NAME      = 12;
    public static final int COL_RECEIVER_COUNTRY   = 13;
    public static final int COL_RECEIVER_ADDRESS   = 14;
    public static final int COL_RECEIVER_CITY      = 15;
    public static final int COL_RECEIVER_STATE     = 16;
    public static final int COL_RECEIVER_POSTCODE  = 17;
    public static final int COL_RECEIVER_CONTACT   = 18;
    public static final int COL_RECEIVER_PHONE     = 19;
    public static final int COL_EMAIL              = 20;
    public static final int COL_NB_ITEMS           = 21;
    public static final int COL_GOODS_DESC         = 22;
    public static final int COL_WEIGHT             = 23;
    public static final int COL_CUSTOMS_VALUE      = 24;
    public static final int COL_CURRENCY           = 25;

    public static final String[] COL_HEADERS = {
        "Mawb #",                 // 0
        "Connote #",              // 1
        "Alternate Reference",    // 2
        "Sender Name",            // 3
        "Sender Country",         // 4
        "Sender Address 1",       // 5
        "Sender Location Name",   // 6
        "Sender State",           // 7
        "Sender Postcode",        // 8
        "Sender Contact",         // 9
        "Sender Phone",           // 10
        "Sender Email",           // 11
        "Receiver Name",          // 12
        "Receiver Country",       // 13
        "Receiver Address 1",     // 14
        "Receiver Location Name", // 15
        "Receiver State",         // 16
        "Receiver Postcode",      // 17
        "Receiver Contact",       // 18
        "Receiver Phone",         // 19
        "Receiver Email",         // 20
        "Number of Items",        // 21
        "Goods Description",      // 22
        "Shipment Weight",        // 23
        "Customs Value",          // 24
        "Customs Currency Code"   // 25
    };

    /** Colonnes obligatoires : leur absence bloque tout l'import au niveau fichier. */
    public static final Set<Integer> REQUIRED_COLS = Set.of(
        COL_MAWB, COL_HAWB, COL_RECEIVER_NAME, COL_EMAIL, COL_CUSTOMS_VALUE, COL_WEIGHT
    );
}

package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.payment.GatewayConfigRequest;
import com.medafrica.mavex.dto.payment.GatewayConfigResponse;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;

import java.util.List;

public interface GatewayConfigService {

    List<GatewayConfigResponse> findAll();

    GatewayConfigResponse findById(Long id);

    /** Retourne le gateway actif — utilisé par le service de paiement */
    PaymentGatewayConfig findActiveConfig();

    GatewayConfigResponse create(GatewayConfigRequest request);

    GatewayConfigResponse update(Long id, GatewayConfigRequest request);

    /** Active ce gateway et désactive tous les autres */
    GatewayConfigResponse activate(Long id);

    /** Désactive ce gateway */
    GatewayConfigResponse deactivate(Long id);

    void delete(Long id);
}

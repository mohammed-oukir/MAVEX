package com.medafrica.mavex.service.payment;

import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Génère le reçu de paiement PDF (template payment-receipt.html)
 * pour un Order/PaymentTransaction donnés, via Thymeleaf + openhtmltopdf.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReceiptService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final SpringTemplateEngine templateEngine;

    public byte[] generateReceipt(Order order, PaymentTransaction transaction) {
        try {
            String html = templateEngine.process("payment-receipt", buildContext(order, transaction));
            return renderPdf(html);
        } catch (Exception e) {
            log.error("Échec de la génération du reçu PDF pour order id={}", order.getId(), e);
            throw new IllegalStateException(
                "Impossible de générer le reçu PDF pour la commande #" + order.getId(), e);
        }
    }

    private Context buildContext(Order order, PaymentTransaction transaction) {
        Client client = order.getClient();
        LocalDateTime paidAt = transaction.getPaidAt();
        String amountFormatted = formatAmount(order.getTotalAmount(), transaction.getCurrency());

        Context ctx = new Context();
        ctx.setVariable("clientNom",        client != null ? client.getFullName() : "—");
        ctx.setVariable("clientEmail",      client != null ? client.getEmail()    : "—");
        ctx.setVariable("clientAdresse",    client != null ? buildAddress(order)  : "—");
        ctx.setVariable("hawb",             order.getHawb());
        ctx.setVariable("description",      order.getGoodsDescription());
        ctx.setVariable("montantFormatted", amountFormatted);
        ctx.setVariable("totalFormatted",   amountFormatted);
        ctx.setVariable("commandeId",       order.getId());
        ctx.setVariable("datePaiement",     paidAt != null ? paidAt.format(DATE_FMT) : "—");
        ctx.setVariable("heurePaiement",    paidAt != null ? paidAt.format(TIME_FMT) : "—");
        ctx.setVariable("modePaiement",     formatGateway(transaction.getGateway()));
        ctx.setVariable("gatewayRef",       transaction.getGatewayRef());
        ctx.setVariable("receiptNumber",    buildReceiptNumber(order, paidAt));
        ctx.setVariable("orderNumber",      "#" + order.getId());
        ctx.setVariable("qty",              "1");
        ctx.setVariable("unitPriceFormatted", amountFormatted);
        ctx.setVariable("devise",           transaction.getCurrency());
        return ctx;
    }

    private String buildReceiptNumber(Order order, LocalDateTime paidAt) {
        int year = paidAt != null ? paidAt.getYear() : LocalDate.now().getYear();
        return String.format("RCPT-%d-%06d", year, order.getId());
    }

    /** Même logique que NotificationEmailServiceImpl.buildAddress(). */
    private String buildAddress(Order order) {
        var c  = order.getClient();
        var sb = new StringBuilder();
        if (c.getAddress() != null) sb.append(c.getAddress()).append(", ");
        if (c.getCity()    != null) sb.append(c.getCity()).append(", ");
        if (c.getState()   != null) sb.append(c.getState()).append(" ");
        if (c.getZipCode() != null) sb.append(c.getZipCode());
        return sb.toString().trim().replaceAll(",\\s*$", "");
    }

    private String formatAmount(BigDecimal amount, String currency) {
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        String cur = currency != null ? currency : "USD";
        return value.setScale(2, RoundingMode.HALF_UP) + " " + cur;
    }

    private String formatGateway(PaymentGatewayType gateway) {
        if (gateway == null) return "—";
        return switch (gateway) {
            case MANUEL -> "Manuel";
            case PAYPAL -> "PayPal";
            case CMI    -> "CMI";
            case STRIPE -> "Stripe";
        };
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la conversion HTML → PDF du reçu de paiement", e);
        }
    }
}

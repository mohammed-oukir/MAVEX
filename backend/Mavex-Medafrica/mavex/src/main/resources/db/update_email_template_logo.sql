-- Fix: move fixed blocks (total-box, info-box, signature) outside BODY_START/BODY_END
-- so Quill edits only the narrative text paragraphs without destroying CSS classes
UPDATE email_templates
SET html_content = '<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family: Arial, sans-serif; background:#f4f4f4; color:#333; }
  .wrapper { max-width:600px; margin:0 auto; background:#fff; }
  .header { background:#4B5563; padding:28px 40px; text-align:center; }
  .header-title { font-size:20px; font-weight:bold; color:#ffffff; letter-spacing:0.05em; }
  .header-sub { font-size:12px; color:#D1D5DB; letter-spacing:0.15em; margin-top:6px; }
  .banner { background:#4B5563; padding:24px 40px; border-bottom:3px solid #F97316; }
  .banner h1 { font-size:20px; color:#ffffff; margin-bottom:6px; }
  .banner p { font-size:14px; color:#E5E7EB; line-height:1.6; }
  .content { padding:32px 40px; }
  .greeting { font-size:16px; margin-bottom:24px; color:#333; }
  .body-text { font-size:14px; color:#555; line-height:1.9; margin-bottom:20px; }
  .info-box { background:#f8f8f8; border:1px solid #eee; border-radius:8px; padding:20px; margin-top:16px; margin-bottom:24px; }
  .info-row { display:table; width:100%; padding:8px 0; border-bottom:1px solid #f0f0f0; font-size:14px; }
  .info-row:last-child { border-bottom:none; }
  .info-label { display:table-cell; color:#888; width:50%; }
  .info-value { display:table-cell; font-weight:600; color:#333; width:50%; }
  .total-box { background:#4B5563; border-radius:8px; padding:20px; margin-bottom:24px; display:table; width:100%; }
  .total-left { display:table-cell; vertical-align:middle; }
  .total-right { display:table-cell; vertical-align:middle; text-align:right; }
  .total-label { font-size:14px; color:#E5E7EB; }
  .total-sub { font-size:12px; color:#D1D5DB; margin-top:4px; }
  .total-amount { font-size:28px; font-weight:900; color:#F97316; }
  .signature { border-top:1px solid #eee; padding-top:20px; margin-top:8px; font-size:13px; color:#555; line-height:1.8; }
  .signature strong { color:#333; display:block; font-size:14px; }
  .signature span { font-size:12px; color:#888; letter-spacing:0.05em; text-transform:uppercase; display:block; }
  .footer { background:#4B5563; padding:24px 40px; text-align:center; }
  .footer p { font-size:12px; color:#D1D5DB; line-height:1.7; }
</style>
</head>
<body>
<div class="wrapper">

  <div class="header">
    <div class="header-title">MED AFRICA LOGISTICS</div>
    <div class="header-sub">CUSTOMER SERVICE</div>
  </div>

  <div class="banner">
    <h1>Payment Notice</h1>
    <p>Please review the details below regarding your shipment.</p>
  </div>

  <div class="content">

<!-- BODY_START -->
    <p class="greeting">Dear <strong>{{receiverName}}</strong>,</p>

    <p class="body-text">
      We are <strong>Med Africa Logistics</strong>, in charge of shipping your
      shipment from Morocco and delivering it to your location.
    </p>

    <p class="body-text">
      Please note that your shipment is subject to
      <strong>customs fees</strong> required by U.S. Customs.
    </p>

    <p class="body-text">
      To avoid any delay, we have covered the customs fees on your behalf
      and kindly ask for reimbursement. The total amount is:
    </p>
<!-- BODY_END -->

    <div class="total-box">
      <div class="total-left">
        <div class="total-label">Total Amount Due</div>
        <div class="total-sub">Customs fees</div>
      </div>
      <div class="total-right">
        <div class="total-amount">{{totalAmount}} {{customsCurrency}}</div>
      </div>
    </div>

<!-- BODY_AFTER_START -->
    <p class="body-text">
      We kindly ask you to arrange the payment of this amount
      to Med Africa Logistics at your earliest convenience.
    </p>

    <p class="body-text">
      Please find attached the <strong>consolidated entry summary</strong> for
      your reference. You may use your tracking number to identify the amount
      related to your shipment.
    </p>

    <p class="body-text">
      Please let us know once the payment has been arranged.
    </p>

    <p class="body-text" style="margin-bottom:0;">
      Shipment details:
    </p>
<!-- BODY_AFTER_END -->

    <div class="info-box">
      <div class="info-row">
        <span class="info-label">Tracking number</span>
        <span class="info-value" style="color:#EF4444;">{{hawb}}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Delivery address</span>
        <span class="info-value" style="color:#16A34A;">{{deliveryAddress}}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Phone number</span>
        <span class="info-value" style="color:#EF4444;">{{clientPhone}}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Commodity</span>
        <span class="info-value" style="color:#6B7280;">{{goodsDescription}}</span>
      </div>
    </div>

    <div class="signature">
      <p>Best regards,</p>
      <br/>
      <strong>Ghita DAHBI</strong>
      <span>CS Manager</span>
      <span>Med Africa Logistics</span>
    </div>

  </div>

  <div class="footer">
    <p>
      Med Africa Logistics — Customer Service<br/>
      This email was sent automatically. Please do not reply directly to this message.
    </p>
  </div>

</div>
</body>
</html>'
WHERE type = 'PAYMENT_INVOICE_WITH_AMOUNT';

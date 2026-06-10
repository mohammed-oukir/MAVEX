-- Fix logo: use public URL from medafrica-log.com (works in all email clients, no blocking)
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
  .header img { max-height:60px; width:auto; }
  .header-sub { font-size:12px; color:#D1D5DB; letter-spacing:0.15em; margin-top:8px; }
  .banner { background:linear-gradient(135deg,#6B7280,#4B5563); padding:28px 40px; border-bottom:3px solid #F97316; }
  .banner h1 { font-size:22px; color:#ffffff; margin-bottom:8px; }
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
  .total-amount { font-size:28px; font-weight:900; color:#F97316; }
  .signature { border-top:1px solid #eee; padding-top:20px; margin-top:8px; font-size:13px; color:#555; line-height:1.8; }
  .signature strong { color:#333; display:block; font-size:14px; }
  .signature span { font-size:12px; color:#888; letter-spacing:0.05em; text-transform:uppercase; display:block; }
  .footer { background:#4B5563; padding:24px 40px; text-align:center; }
  .footer p { font-size:12px; color:#D1D5DB; line-height:1.7; }
  .footer a { color:#F97316; text-decoration:none; }
</style>
</head>
<body>
<div class="wrapper">

  <div class="header">
    <img src="https://medafrica-log.com/wp-content/uploads/2023/03/MEDAF-WWEXPRESS10.png" alt="Med Africa Logistics" style="max-height:60px;width:auto;"/>
    <div class="header-sub">MED AFRICA LOGISTICS</div>
  </div>

  <div class="banner">
    <h1>Payment Notice</h1>
    <p>Action required — please review the details below.</p>
  </div>

  <div class="content">

    <p class="greeting">Dear <strong>{{receiverName}}</strong>,</p>

    <p class="body-text">
      We are <strong>Med Africa Logistics</strong>, in charge of shipping your
      shipment from Morocco and delivering it to your location.
    </p>

    <p class="body-text">
      Please note that your shipment purchased from Morocco is subject to
      <strong>customs duties and taxes</strong> required by U.S. Customs.
    </p>

    <p class="body-text">
      To avoid any delay, we have proceeded with the payment on your behalf
      while awaiting reimbursement from you. Kindly note that the total amount
      of duties and taxes is:
    </p>

    <div class="total-box">
      <div class="total-left">
        <div class="total-label">Total Amount Due</div>
        <div style="font-size:12px;color:#D1D5DB;margin-top:4px;">Customs duties &amp; taxes</div>
      </div>
      <div class="total-right">
        <div class="total-amount">{{totalAmount}} {{customsCurrency}}</div>
      </div>
    </div>

    <p class="body-text">
      We would appreciate your <strong>urgent payment</strong> of this amount
      to Med Africa Logistics.
    </p>

    <p class="body-text">
      Please find attached the <strong>consolidated entry summary</strong> for
      your reference. You may use your tracking number to identify the amount
      related to your shipment.
    </p>

    <p class="body-text">
      Please kindly advise us once the payment has been made.
    </p>

    <p class="body-text" style="margin-bottom:0;">
      Please find below the details of your shipment:
    </p>

    <div class="info-box">
      <div class="info-row">
        <span class="info-label">Your tracking number is</span>
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
      <p>Best regards / Meilleures salutations,</p>
      <br/>
      <strong>Ghita DAHBI</strong>
      <span>CS Manager</span>
      <span>Med Africa Logistics</span>
    </div>

  </div>

  <div class="footer">
    <p>
      Med Africa Logistics — Customer Service<br/>
      This email was sent automatically, please do not reply.<br/>
      <a href="#">Privacy Policy</a>
    </p>
  </div>

</div>
</body>
</html>'
WHERE type = 'PAYMENT_INVOICE_WITH_AMOUNT';

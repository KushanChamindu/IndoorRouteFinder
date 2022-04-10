package com.example.indoorroutefinder.utils.QRReader;


public interface QRCodeFoundListener {
    void onQRCodeFound(String qrCode);
    void qrCodeNotFound();
}
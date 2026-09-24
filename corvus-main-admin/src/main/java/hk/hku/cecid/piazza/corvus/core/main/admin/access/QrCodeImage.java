package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.nayuki.qrcodegen.QrCode;

/** QR codes as SVG data URIs, generated locally (no outside service). */
final class QrCodeImage {

    private QrCodeImage() {
    }

    static String dataUri(String text) {
        QrCode qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
        int border = 4;
        int size = qr.size + border * 2;
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                if (qr.getModule(x, y)) {
                    path.append('M').append(x + border).append(',').append(y + border).append("h1v1h-1z");
                }
            }
        }
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + size + " " + size
                + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<path d=\"" + path + "\" fill=\"#000\"/></svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }
}

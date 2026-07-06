/*
 *
 */

package inko;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 1.0.0
 *
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import static inko.ImageUtility.convertBytesToImage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.*;
import util.EnumHelper;

public class SignatureServer {

    public final static int PORT = 8080;
    public final static String PARAM1 = "patientId";
    public final static String PARAM2 = "document";

    private final MainFrame mainFrame;

    private HttpServer server;

    public SignatureServer(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public static String getLocalIpAddress(String pattern) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String[] patternArray = { pattern, "192.168.", "10.", "172." };
            for (String s : patternArray) {
                String ip = filterHostAddress(interfaces, s);
                if (ip != null) {
                    return ip;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        // Fallback, falls gar nichts gefunden wurde
        return "127.0.0.1";
    }

    public static String filterHostAddress(Enumeration<NetworkInterface> interfaces, String pattern) throws SocketException {
        if (pattern == null || pattern.length() <= 0) {
            return null;
        }

        for ( NetworkInterface netInterface : Collections.list( interfaces )) {
            // Ignoriere inaktive Schnittstellen, Loopbacks und virtuelle Docker/VM-Adapter
            if ( !netInterface.isUp() || netInterface.isLoopback() || netInterface.isVirtual() ) {
                continue;
            }

            Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
            for ( InetAddress addr : Collections.list( addresses )) {
                // nur eine echte IPv4-Adresse
                if ( addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress() ) {
                    String hostAddress = addr.getHostAddress();
                    // filtern
                    if ( hostAddress.startsWith( pattern )) {
                        return hostAddress;
                    }
                }
            }
        }
        return null;
    }

    public Patient getPatient() {
        return mainFrame.patientTableModel.getPatient();
    }

    private Patient getPatientById(int id) {
        return mainFrame.getPatients().get(id);
    }

    private void refreshUI(Patient patient) {
        /* Da der HttpServer in einem Hintergrundthread läuft,
         * musst du GUI-Updates in Swing über den EventQueue/EDT jagen:
         * java.awt.EventQueue.invokeLater(() -> { meinLabel.repaint(); });
         */
    }

    public void startServer() throws IOException {
        server = HttpServer.create( new InetSocketAddress( PORT ), 0 );
        server.createContext( "/signature", new SignatureHandler() ); // Kontext für die Unterschriften-Seite
        server.setExecutor(null); // Standard-Executor nutzen
        server.start();
        System.out.println("SignatureServer gestartet an Port " + PORT + ".");
    }

    public HttpServer getServer() {
        return server;
    }

    public void stopServer(int i) {
        System.out.println("stoppe SignatureServer an Port " + PORT + " in " + i + " Sekunden.");
        server.stop(i);
    }

    private static Map<String, String> parseQueryParameters(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }
        }
        return result;
    }

    // Hilfsmethode zum Senden von Antworten
    private static void sendResponse(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] response = text.getBytes(UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        try ( OutputStream os = exchange.getResponseBody() ) {
            os.write(response);
        }
    }

    private class SignatureHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            URI requestedUri = exchange.getRequestURI();

            // Parameter aus der URL parsen
            Map<String, String> params = parseQueryParameters( requestedUri.getQuery() );
            int patientId = 0;
            SignableDocument document = null;
            try {
                patientId = Integer.parseInt( params.get( PARAM1 ));
                document  = EnumHelper.getEnumFromString( SignableDocument.class, params.get( PARAM2 ));
            } catch (NumberFormatException | NullPointerException e) {
                System.err.println( "Fehler Signature-Server: " + e.getMessage() );
            }

            if ( "GET".equalsIgnoreCase( method )) {
                // HTML-Seite ausliefern
                String html = getHtmlTemplate();
                byte[] response = html.getBytes(UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, response.length);
                try ( OutputStream os = exchange.getResponseBody() ) {
                    os.write(response);
                }
            } else if ( "POST".equalsIgnoreCase( method )) {
                if ( patientId < 1 || document == null ) {
                    sendResponse(exchange, 400, "Fehler: Patienten-ID oder Dokument ungültig oder fehlt.");
                    return;
                }
                // PNG-Daten empfangen
                InputStream in = exchange.getRequestBody();
                BufferedReader reader = new BufferedReader( new InputStreamReader( in, UTF_8 ));

                StringBuilder json = new StringBuilder();
                String line;
                while (( line = reader.readLine() ) != null ) {
                    json.append(line);
                }

                // simples Extrahieren des Base64-Strings aus dem JSON
                // (In der Praxis gerne einen echten JSON-Parser oder präzisen Regex nutzen)
                String payload = json.toString();
                if ( payload.contains( "image/png;base64" )) {
                    // Base64 extrahieren und dekodieren
                    String base64Data = payload.split("image/png;base64,")[1].split("\"")[0];
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                    BufferedImage signImage = convertBytesToImage(imageBytes);
                    Patient patient = getPatient();
                    if (patient != null) {
                        patient.setSignature( new Signature( document, signImage ));
                        refreshUI(patient);
                        sendResponse(exchange, 200, "OK");
                    } else {
                        sendResponse(exchange, 404, "Patient nicht gefunden / geladen.");
                    }
                } else {
                    sendResponse(exchange, 400, "Ungültige Bilddaten.");
                }
            }
        }

        private String getHtmlTemplate() {
            try ( InputStream in = getClass().getResourceAsStream( "/inko/PNGSigner.html" )) {
                if (in == null) {
                    System.err.println("Fehler: PNGSigner.html nicht gefunden.");
                    return "<h1>Server-Fehler: Vorlage fehlt.</h1>";
                }
                try ( BufferedReader reader = new BufferedReader( new InputStreamReader( in, UTF_8 ))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while (( line = reader.readLine() ) != null ) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "<h1>Server-Fehler beim Laden der Vorlage.</h1>";
            }
        }
    }

}

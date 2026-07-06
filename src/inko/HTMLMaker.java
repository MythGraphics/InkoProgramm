/*
 *
 */

package inko;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 4.0.0
 *
 */

import dataformat.xml.html.TableRow;
import java.awt.Desktop;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class HTMLMaker {

    public final static String TEMPLATE_DELIMITER   = "⚕";
    public final static String MARKER               = "<!-- Data here -->";

    public final static String HEADERCELL_START     = "<th>";
    public final static String HEADERCELL_END       = "</th>";
    public final static String TABLEROW_START       = "<tr>";
    public final static String TABLEROW_END         = "</tr>";
    public final static String CELL_START           = "<td>";
    public final static String CELLCLASS_START      = "<td class=\"";
    public final static String CELL_END             = "</td>";
    public final static String NEWLINE              = "<br />";

    public final static String HEADER_LIEFERLISTE   =
        "<tr>" +
        "<th class=\"nr\">Nr.</th>" +
        "<th class=\"frei\">F</th>" +
        "<th class=\"name\">Name, Vorname</th>" +
        "<th class=\"anschrift\">Anschrift</th>" +
        "<th class=\"ort\">Ort</th>" +
        "<th class=\"kommentar\">Kommentar</th>" +
        "<th class=\"tel\">Tel.-Nr.</th>" +
        "<th class=\"artikel\">Artikel</th>" +
        "<th class=\"preis\">Preis</th>" +
        "</tr>"
    ;

    public final static int EXPIRINGLIST      = 3;
    public final static int EXPIRINGLIST_RX   = 31;
    public final static int EXPIRINGLIST_BIND = 32;

    public final static String IOERROR = "Erstellen der Liste fehlgeschlagen (I/O Fehler)";

    private final File template;
    private final File target;

    public HTMLMaker(String template, String target) {
        this.template = new File(template);
        this.target   = new File(target);
    }

    public BufferedReader getTextReader(File source) throws FileNotFoundException {
        return new BufferedReader( new InputStreamReader( new FileInputStream( source ), StandardCharsets.UTF_8 ));
    }

    public static PrintWriter getTextWriter(File target) throws IOException {
        return new PrintWriter( Files.newBufferedWriter( target.toPath(), StandardCharsets.UTF_8 ));
    }

    public static void makeHtmlFile(File target, String content) throws IOException {
        target.createNewFile();
        try ( PrintWriter out = getTextWriter( target )) {
            out.println(content);
            out.flush();
        }
        try { Desktop.getDesktop().browse( target.toURI() ); }
        catch (IOException e) { e.printStackTrace(); }
        try { Thread.sleep(500); }
        catch (InterruptedException e) {}
    }

    private static String getLeadingWhitespace(String line) {
        return line.substring( 0, line.indexOf( '<' ));
    }

    public void makeFrontpage(Patient p) throws IOException {
        StringBuilder sb = new StringBuilder();
        try ( BufferedReader in = getTextReader( template )) {
            while ( in.ready() ) {
                String line = in.readLine();
                if ( !line.contains( MARKER )) {
                    line = Patient.replaceTemplate(line, p);
                    sb.append(line).append("\n");
                    continue;
                }
                String leader = getLeadingWhitespace(line);
                for ( Artikel h : p.getArtikelList() ) {
                    sb.append(leader).append("<table><tr><th class=\"largebold\">");
                    sb.append( h.getFullArtikelName() );
                    sb.append("</th><th class=\"smallbold\">");
                    sb.append( h.getPZN() );
                    sb.append("</th></tr></table>").append("\n");
                }
            }
        }
        makeHtmlFile( target, sb.toString() );
    }

    public void makeSignedDocument(Patient p, SignableDocument document) throws IOException {
        StringBuilder sb = new StringBuilder();
        try ( BufferedReader in = getTextReader( template )) {
            while ( in.ready() ) {
                String line = in.readLine();
                if ( line.contains( TEMPLATE_DELIMITER )) {
                    line = Patient.replaceTemplate(line, p);
                }
                sb.append(line).append("\n");
            }
        }
        makeHtmlFile( target, sb.toString() );
    }

    public void makeArtikelList2(List<? extends Patient> pats) throws IOException {
        StringBuilder sb = new StringBuilder();
        try ( BufferedReader in = getTextReader( template )) {
            while ( in.ready() ) {
                String line = in.readLine();
                if ( !line.contains( MARKER )) {
                    sb.append(line).append("\n");
                    continue;
                }
                String leader = getLeadingWhitespace(line);
                for (Patient p : pats) {
                    if ( p.isPaused() ) {
                        continue;
                    }
                    String artikel = p.getArtikelListAsString().replaceAll(Patient.ARTIKEL_SEPARATOR, "<br>");
                    sb.append(leader).append("<tr><td class=\"name\">");
                    sb.append( p.getFullName() );
                    sb.append("</td><td class=\"artikel\">");
                    sb.append(artikel);
                    sb.append("</td></tr>").append("\n");
                }
            }
        }
        makeHtmlFile( target, sb.toString() );
    }

    public void makeArtikelList(List<? extends Patient> pats, List<Artikel> artikelList)
    throws IOException, IllegalArgumentException {
        HashMap<Integer, Integer> mergeMap = new HashMap<>(); // Artikel.ID -> Artikel-Menge pro Patient
        HashMap<Integer, String> stringMap = new HashMap<>(); // Artikel.ID -> Artikel.NAME
        StringBuilder sb = new StringBuilder();
        try ( BufferedReader in = getTextReader( template )) {
            while ( in.ready() ) {
                String line = in.readLine();
                if ( !line.contains( MARKER )) {
                    sb.append(line).append("\n");
                    continue;
                }
                String leader = getLeadingWhitespace(line);
                for ( Patient p : pats ) {
                    if ( p.isPaused() ) {
                        continue;
                    }
                    for ( Artikel a : p.getArtikelList() ) {
                        mergeMap.merge( a.getId(), a.getMenge(), Integer::sum );
                    }
                }
                for ( Artikel a : artikelList ) {
                    stringMap.putIfAbsent( a.getId(), a.getReducedArtikelName() );
                }
                TreeMap<Integer, String> sortedMap = new TreeMap<>(stringMap);
                for ( Map.Entry<Integer, String> entry : sortedMap.entrySet() ) {
                    Integer i = mergeMap.get( entry.getKey() );
                    if ( i == null || i == 0 ) {
                        // Menge == 0 -> Eintrag übererspringen
                        continue;
                    }
                    sb.append(leader).append("<tr><td class=\"menge\">");
                    sb.append( mergeMap.get( entry.getKey() )).append("x");
                    sb.append("</td><td class=\"artikel\">");
                    sb.append( entry.getValue() );
                    sb.append("</td></tr>").append("\n");
                }
            }
        }
        makeHtmlFile( target, sb.toString() );
    }

    public void makeExpiringList(List<Patient> pats, int listType) throws IOException {
        StringBuilder sb = new StringBuilder();
        try ( BufferedReader in = getTextReader( template )) {
            while ( in.ready() ) {
                String line = in.readLine();
                if ( !line.contains( MARKER )) {
                    sb.append(line).append("\n");
                    continue;
                }
                String leader = getLeadingWhitespace(line);
                for (Patient p : pats) {
                    if ( p.isPaused() ) {
                        continue;
                    }
                    String[] content = null;
                    switch (listType) {
                        case EXPIRINGLIST:
                            if ( p.isBindingExpiringSoon() || p.isPrescriptionExpiringSoon() ) {
                                content = new String[] {
                                    p.getLastName()  + ", " + p.getFirstName(),
                                    p.getBindingExpiringDate().format(Patient.DEFAULT_FORMATTER),
                                    p.getPrescriptionExpiringDate().format(Patient.DEFAULT_FORMATTER)
                                };
                            }
                            break;
                        case EXPIRINGLIST_RX:
                            if ( p.isPrescriptionExpiringSoon() ) {
                                content = new String[] {
                                    p.getLastName()  + ", " + p.getFirstName(),
                                    p.getPrescriptionExpiringDate().format(Patient.DEFAULT_FORMATTER)
                                };
                            }
                            break;
                        case EXPIRINGLIST_BIND:
                            if ( p.isPrescriptionExpiringSoon() ) {
                                content = new String[] {
                                    p.getLastName()  + ", " + p.getFirstName(),
                                    p.getBindingExpiringDate().format(Patient.DEFAULT_FORMATTER)
                                };
                            }
                            break;
                    }
                    if (content != null) {
                        sb.append(leader).append( new TableRow( content, null, false ).toString() );
                    }
                    sb.append("\n");
                }
            }
        }
        makeHtmlFile( target, sb.toString() );
    }

    public void makeDeliveryList(List<? extends Patient> pats) throws IOException {
        StringBuilder sb = new StringBuilder();
        try ( BufferedReader in = getTextReader( template )) {
            while ( in.ready() ) {
                String line = in.readLine();
                if ( !line.contains( MARKER )) {
                    sb.append(line);
                    sb.append("\n");
                    continue;
                }
                String leader = getLeadingWhitespace(line);
                String bigleader = "    " + leader;
                List<Patient> list = new ArrayList<>();
                for (Patient p : pats) {
                    if ( !p.isPaused() && p.toDeliver() ) {
                        list.add(p);
                    }
                }
                sb.append(leader).append("<b>Liste 1 / Tour 1:</b>").append("\n");
                sb.append(leader).append("<table>").append("\n");
                sb.append(bigleader).append(HEADER_LIEFERLISTE).append("\n");

                // sortieren nach Ort
                list.sort(( Patient p1, Patient p2 ) ->
                    new Location( p1 ).compareTo( new Location( p2 ))
                );
                // in HTML formatieren
                boolean flag = true;
                String frei, artikelList;
                Patient p;
                for ( int i = 0; i < list.size(); ++i ) {
                    artikelList = list.get(i).getArtikelListAsString();
                    p = list.get(i);
                    if ( p.isCoPaymentFree() ) {
                        frei = "X";
                    } else {
                        frei = "";
                    }
                    String[] content = {
                        String.valueOf(i+1), // Zeilen-Nummerierung
                        frei,
                        p.getFullName(),
                        p.getStreet(),
                        p.getCity(),
                        p.getComment(),
                        p.getPhoneNumber(),
                        artikelList.replaceAll(Patient.ARTIKEL_SEPARATOR, "<br>"),
                        "" // leere Spalte für Preis
                    };
                    if ( flag && p.getCityAsLocation().isLocal() ) {
                        sb.append(leader).append("</table>").append("\n");
                        sb.append(leader).append("<br />" ).append("\n");
                        sb.append(leader).append("<b>Liste 2 / Tour 2:</b>").append("\n");
                        sb.append(leader).append("<table>").append("\n");
                        sb.append(bigleader).append(HEADER_LIEFERLISTE).append("\n");
                        flag = false;
                    }
                    sb.append(leader).append(TABLEROW_START);
                    for (int j = 0; j < content.length; ++j) {
                        if ( j == 5 ) { // "Kommentar-Spalte"
                            sb.append("<td class=\"klein\">");
                        } else {
                            sb.append(CELL_START);
                        }
                        sb.append(content[j]);
                        sb.append(CELL_END);
                    }
                    sb.append(TABLEROW_END).append("\n");
                }
                sb.append(leader).append("</table>").append("\n");
            }
        }
        makeHtmlFile( target, sb.toString() );
    }

    public void makePatientList(List<Patient> pats) throws IOException {
        StringBuilder sb = new StringBuilder();
        try ( BufferedReader in = getTextReader( template )) {
            while ( in.ready() ) {
                String line = in.readLine();
                if ( !line.contains( MARKER )) {
                    sb.append(line).append("\n");
                    continue;
                }
                String leader = getLeadingWhitespace(line);
                for (Patient p : pats) {
                    if ( p.isPaused() ) {
                        continue;
                    }
                    sb.append(leader).append("<tr><td class=\"small\">");
                    if ( p.isCoPaymentFree() ) {
                        sb.append("X");
                    }
                    sb.append("</td><td class=\"small\">");
                    sb.append( String.valueOf( p.getType().getCode() ).toUpperCase() );
                    sb.append("</td><td class=\"name\">");
                    sb.append( p.getFullName() );
                    sb.append("</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>").append("\n");
                }
            }
        }
        makeHtmlFile( target, sb.toString() );
    }

}

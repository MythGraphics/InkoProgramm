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

public enum Document {

    DECKBLATT               ("deckblatt.html"),
    BERATUNGSBOGEN          ("beratungsbogen.html"),
    BINDUNGSERKLÄRUNG       ("bindungserklärung.html"),
    MEHRKOSTENERKLÄRUNG     ("mehrkosten.html"),
    FÄLLIGKEITSLISTE        ("faellig.html"),
    FÄLLIGKEITSLISTE_RX     ("faellig_rx.html"),
    FÄLLIGKEITSLISTE_BIND   ("faellig_bind.html"),
    LIEFERLISTE             ("lieferliste.html"),
    PATIENTENLISTE          ("patientenliste_rezeptabrechnung.html"),
    HIMILISTE               ("himiliste.html"),
    HIMILISTE2              ("himiliste_erw.html");

    private final String filename;

    Document(String filename) {
        this.filename = filename;
    }

    public String getFileName() {
        return filename;
    }

}

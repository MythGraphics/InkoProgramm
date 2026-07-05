/*
 *
 */

package inko;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 2.0.0
 *
 */

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Definiert alle verfügbaren Felder eines Patienten inklusive ihrer
 * SQL-Spaltennamen, UI-Bezeichner (deutsche Bezeichnung), Datentypen und Dokument-Templates.
 */
public enum PatientField {

    // --- DATENBANK FELDER ---
    ID("id", "int unsigned not null auto_increment primary key", "ID", Integer.class, "⚕nr⚕"),
    LAST_NAME("last_name", "tinytext", "Familienname", String.class, "⚕name⚕"),
    FIRST_NAME("first_name", "tinytext", "Vorname", String.class, "⚕vorname⚕"),
    STREET("street", "tinytext", "Straße", String.class, "⚕straße⚕"),
    POSTCODE("postcode", "int(5) unsigned", "PLZ", String.class, "⚕plz⚕"),
    CITY("city", "tinytext", "Wohnort", String.class, "⚕ort⚕"),
    BIRTHDATE("birthday", "date", "Geburtsdatum", LocalDate.class, "⚕gdatum⚕"),
    HEALTH_INSURENCE_IK("kk_ik", "int(9) unsigned", "IK Krankenkassen", Integer.class, "⚕kk⚕"),
    HEALTH_INSURENCE_NUMBER("kv_number", "tinytext", "KV-Nummer", String.class, "⚕kvn⚕"),
    PHONE("telefon", "tinytext", "Telefon-Nummer", String.class, "⚕tel⚕"),
    COMMENT("comment", "text", "Kommentar", String.class, "⚕kommentar⚕"),
    RX_DATE("rx_date", "date", "Rezeptdatum", LocalDate.class, "⚕rxdatum⚕"),
    FIRST_SUPPLY_DATE("erstbelieferung", "date", "Erstbelieferung", LocalDate.class, "⚕erstbelieferung⚕"),
    END_OF_LICENCE_DATE("ende_genehmigung", "date", "Ende Genehmigungszeitraum", LocalDate.class, "⚕gzeit⚕"),
    END_OF_BINDING_DATE("ende_bindung", "date", "Ende Bindungszeitraum", LocalDate.class, "⚕bzeit⚕"),
    DELIVER("liefern", "boolean", "Liefern?", Boolean.class, "⚕liefern⚕"),
    BEFREIUNGSDATUM("befreiungsdatum", "date", "befreit bis", LocalDate.class, "⚕frei⚕"),
    TYP("typ", "character(1)", "Typ-Zeichen", String.class, "⚕typ⚕"), // PatientType.getCode()
    MENGENLISTE("artikelmenge", "tinytext", "Artikelmengenliste", String.class, "⚕mengen_liste⚕"),
    ARTIKELLISTE("artikelliste", "tinytext", "Artikelliste", String.class, "⚕artikel_liste⚕"),
    PAUSE("pause", "boolean", "Patient pausiert?", Boolean.class, "⚕pause⚕"),

    // --- TEMPLATE FELDER (existieren so nicht in der DB) ---
    BESONDERHEITEN(null, null, "Besonderheiten", String.class, "⚕besonderheiten⚕"),
    ACTK(null, null, "ACTK", String.class, "⚕actk⚕"),
    KK_NAME(null, null, "Krankenkasse", String.class, "⚕kk_name⚕"),
    TYPE_LABEL(null, null, "Typ", String.class, "⚕typ_str⚕"), // PatientType.getLabel()
    HIMI(null, null, "Hilfsmittel", String.class, "⚕hm⚕"); // getHimiListAsString()

    // Performance-Optimierung: Statische Listen für Java 8
    public final static List<PatientField> DB_FIELDS;
    public final static List<PatientField> INSERT_FIELDS;
    public final static List<PatientField> UI_FIELDS;
    public final static List<PatientField> SHORT_DATE_FIELDS;
    public final static List<PatientField> ADG_FIELDS;
    public final static String UI_FIELD_STRING;
    public final static String INSERT_COLUMNS;
    public final static String INSERT_PLACEHOLDERS;

    static {
        // alle Felder, die eine Spalte in der DB haben
        DB_FIELDS = Collections.unmodifiableList(
            Arrays.stream( values() )
                  .filter(f -> f.dbName != null)
                  .collect( Collectors.toList() )
        );
        // alle DB-Felder außer ID (für Insert)
        INSERT_FIELDS = Collections.unmodifiableList(
            DB_FIELDS.stream()
                     .filter(f -> f != ID)
                     .collect( Collectors.toList() )
        );
        // alle Felder der UI-Tabelle, die das verkürzte Datum (MM.JJJJ) anzeigen sollen
        SHORT_DATE_FIELDS = Collections.unmodifiableList( Arrays.asList(
            END_OF_LICENCE_DATE,
            END_OF_BINDING_DATE,
            BEFREIUNGSDATUM
        ));
        // alle DB-Felder der UI-Tabelle
        UI_FIELDS = Collections.unmodifiableList( Arrays.asList(
            ID,
            LAST_NAME,
            FIRST_NAME,
            STREET,
            POSTCODE,
            CITY,
            BIRTHDATE,
            HEALTH_INSURENCE_IK,
            HEALTH_INSURENCE_NUMBER,
            PHONE,
            COMMENT,
            RX_DATE,
            FIRST_SUPPLY_DATE,
            END_OF_LICENCE_DATE,
            END_OF_BINDING_DATE
        ));
        ADG_FIELDS = Collections.unmodifiableList( Arrays.asList(
            LAST_NAME,
            FIRST_NAME,
            STREET,
            POSTCODE,
            CITY,
            BIRTHDATE,
            HEALTH_INSURENCE_IK,
            HEALTH_INSURENCE_NUMBER,
            PHONE,
            BEFREIUNGSDATUM
        ));

        UI_FIELD_STRING = INSERT_FIELDS.stream()
            .map(f -> f.uiName)
            .collect( Collectors.joining( "\n" ));

        INSERT_COLUMNS = INSERT_FIELDS.stream()
            .map(f -> f.dbName)
            .collect( Collectors.joining( ", " ));

        INSERT_PLACEHOLDERS = INSERT_FIELDS.stream()
            .map(f -> "?")
            .collect( Collectors.joining( ", " ));
    }

    private final String dbName;
    private final String dbType;
    private final String uiName;
    private final Class<?> type;
    private final String template;

    PatientField(String dbName, String dbType, String uiName, Class<?> type, String template) {
        this.dbName = dbName;
        this.dbType = dbType;
        this.uiName = uiName;
        this.type = type;
        this.template = template;
    }

    public String getDBName() {
        return dbName;
    }

    public String getDBType() {
        return dbType;
    }

    public String getUIName() {
        return uiName;
    }

    public Class<?> getType() {
        return type;
    }

    public String getTemplate() {
        return template;
    }

}

/*
 *
 */

package inko;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 7.0.0
 *
 */

import static inko.ImageUtility.convertToHtmlBase64;
import static inko.InkoType.SAUGEND;
import static inko.PatientField.*;
import static inko.SignableDocument.*;
import static inko.SignatureField.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Patient implements Comparable<Patient>, HasArtikel {

    // Formatter für die verschiedenen Anwendungsfälle
    public final static DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public final static DateTimeFormatter REDUCED_FORMATTER = DateTimeFormatter.ofPattern("MM.yyyy");
    public final static DateTimeFormatter SQL_FORMATTER     = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public final static LocalDate EOT = LocalDate.of(2999, 12, 31);
    public final static LocalDate DEFAULT_DATE = LocalDate.of(1900, 1, 1);

    public final static String ARTIKEL_SEPARATOR = ", ";

    public final static Map<String, Enum> TAG_MAP = initTagMap();

    // sucht alles zwischen zwei ⚕
    private final static Pattern TEMPLATE_PATTERN = Pattern.compile("⚕([^⚕]+)⚕");

    private int id                                      = -1;
    private String lastName                             = "";
    private String firstName                            = "";
    private String street                               = "";
    private Integer postCode                            = 0;
    private String city                                 = "";
    private LocalDate birthDate                         = DEFAULT_DATE;
    private Integer healthInsurenceIK                   = 0;
    private String insurenceNumber                      = ""; // als String, da sie immer mit einem Buchstaben beginnt
    private String phoneNumber                          = "";
    private String comment                              = "";
    private LocalDate prescriptionDate                  = DEFAULT_DATE;
    private LocalDate firstSupplyDate                   = DEFAULT_DATE;
    private LocalDate prescriptionExpiringDate          = DEFAULT_DATE;
    private LocalDate bindingExpiringDate               = DEFAULT_DATE;
    private LocalDate coPaymentFreeUntil                = DEFAULT_DATE;
    private InkoType type                               = SAUGEND;
    private List<Artikel> itemList                      = new ArrayList<>();
    private List<Integer> itemIdList                    = new ArrayList<>();
    private List<Integer> itemQuantityList              = new ArrayList<>();
    private Map<SignableDocument, Signature> signMap    = new HashMap<>();
    private boolean deliver                             = false;
    private boolean paused                              = false;
    private boolean modified                            = false; // NUR für DB-Einträge

    public Patient() {}

    private static Map<String, Enum> initTagMap() {
        Map<String, Enum> map = new HashMap<>();
        map.putAll(
            Arrays.stream( PatientField.values() )
                  .filter( f -> f.getTemplate() != null )
                  .collect( Collectors.toMap(
                      f -> f.getTemplate().replace("⚕", ""), // "⚕name⚕" -> "name"
                      f -> f
        )));
        map.putAll(
            Arrays.stream( SignatureField.values() )
                  .filter( f -> f.getTemplate() != null )
                  .collect( Collectors.toMap(
                      f -> f.getTemplate().replace("⚕", ""), // "⚕name⚕" -> "name"
                      f -> f
        )));
        return map;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isModified() {
        return modified;
    }

    public void setComment(String newComment) {
        this.comment = (newComment == null) ? "" : newComment;
    }

    public String getComment() {
        return comment;
    }

    public boolean hasSignatureData() {
        return signMap != null && !signMap.isEmpty();
    }

    public Signature getSignature(SignableDocument document) {
        return signMap.get(document);
    }

    public void setSignature(Signature sign) {
        signMap.put( sign.getDocumentType(), sign );
        setModified(true);
    }

    public void setSignatureMap(Map<SignableDocument, Signature> signMap) {
        this.signMap = signMap;
        // wird von DBio beim Laden des Patienten aus der Datenbank aufgerufen und setzt daher modified nicht
    }

    /**
     * Prüft, ob die Bindungserklärung im nächsten Monat ausläuft.
     * @return WAHR oder FALSCH
     */
    public boolean isBindingExpiringSoon() {
        LocalDate threshold = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        return bindingExpiringDate != null && bindingExpiringDate.isBefore(threshold);
    }

    /**
     * Prüft, ob Rezept/Genehmigung im nächsten Monat ausläuft.
     * @return WAHR oder FALSCH
     */
    public boolean isPrescriptionExpiringSoon() {
        LocalDate threshold = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        return prescriptionExpiringDate != null && prescriptionExpiringDate.isBefore(threshold);
    }

    /**
     * Prüft, ob die Bindungserklärung bereits abgelaufen ist.
     * @return WAHR oder FALSCH
     */
    public boolean isBindingExpired() {
        return bindingExpiringDate != null && bindingExpiringDate.isBefore( LocalDate.now() );
    }

    /**
     * Prüft, ob Rezept/Genehmigung bereits abgelaufen ist.
     * @return WAHR oder FALSCH
     */
    public boolean isPrescriptionExpired() {
        return prescriptionExpiringDate != null && prescriptionExpiringDate.isBefore( LocalDate.now() );
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFullName() {
        return lastName + ", " + firstName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getPrescriptionDate() {
        return prescriptionDate;
    }

    public LocalDate getFirstSupplyDate() {
        return firstSupplyDate;
    }

    public LocalDate getPrescriptionExpiringDate() {
        return prescriptionExpiringDate;
    }

    public LocalDate getBindingExpiringDate() {
        return bindingExpiringDate;
    }

    public LocalDate getCoPaymentFreeUntil() {
        return coPaymentFreeUntil;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public boolean isCoPaymentFree() {
        return coPaymentFreeUntil.isAfter( LocalDate.now() );
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public Location getCityAsLocation() {
        return new Location( getCity() );
    }

    public String getInsurenceNumber() {
        return insurenceNumber;
    }

    public String getHealthInsurer() {
        return InsurenceCompany.getByIK(healthInsurenceIK).getName();
    }

    public int getHealthInsurenceIK() {
        return healthInsurenceIK;
    }

    public InkoType getType() {
        return type;
    }

    public void setType(InkoType type) {
        this.type = type;
        setModified(true);
    }

    public String getBesonderheiten() {
        return InsurenceCompany.getByIK(healthInsurenceIK).getInfo(type);
    }

    public String getACTK() {
        return InsurenceCompany.getByIK(healthInsurenceIK).getACTK(type);
    }

    /**
     * Postleitzahl (PLZ)
     * @return PLZ
     */
    public int getPostCode() {
        return postCode;
    }

    public boolean toDeliver() {
        return deliver;
    }

    public Artikel getArtikel(int id) {
        try {
            return getArtikelList().get(id);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Artikel> getArtikelList() {
        return itemList;
    }

    @Override
    public String getArtikelListAsString() {
        return getArtikelList().stream()
                               .map(Artikel::getFullArtikelString)
                               .collect( Collectors.joining( ARTIKEL_SEPARATOR ));
    }

    /**
     * Aktualisiert <code>artikelIdList</code> & <code>mengenList</code> mittels <code>artiekList</code> und setzt
     * <code>modified</code> auf TRUE.
     */
    public void refreshArtikelList() {
        itemIdList.clear();
        itemQuantityList.clear();
        for ( Artikel a : getArtikelList() ) {
            itemIdList.add( a.getId() );
            itemQuantityList.add( a.getMenge() );
        }
        setModified(true);
    }

    /**
     * Erstellt die ArtikelListe aus <code>artikelIdList</code> und <code>mengenList</code>.
     * @param allItemList Liste aller Artikel
     */
    public void buildArtikelList(List<Artikel> allItemList) {
        Artikel a;
        for (int j = 0; j < itemIdList.size(); ++j) {
            for (int i = 0; i < allItemList.size(); ++i) {
                if ( allItemList.get( i ).getId() == itemIdList.get( j )) {
                    a = allItemList.get(i).clone();
                    a.setMenge( itemQuantityList.get( j ));
                    itemList.add(a);
                }
            }
        }
    }

    @Override
    public int compareTo(Patient other) {
        if (other == null) {
            return 1;
        }
        return Comparator.comparing(Patient::getLastName)
                         .thenComparing(Patient::getFirstName)
                         .compare(this, other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ( !( obj instanceof Patient )) {
            return false;
        }
        Patient patient = (Patient) obj;
        return Objects.equals(lastName, patient.lastName)   &&
               Objects.equals(firstName, patient.firstName) &&
               Objects.equals(birthDate, patient.birthDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastName, firstName, birthDate);
    }

    @Override
    public String toString() {
        return getFullName();
    }

    /**
     * Erzeugt ein Patient-Objekt aus dem Datensatz der ADG-Liste (Legacy-Support).
     * @param row Ein Array mit Strings
     * @return Ein initialisiertes Patient-Objekt
     */
    static Patient fromADG(String[] row) {
        Patient p = new Patient();
        try {
            p.set( LAST_NAME,                   row[0] );
            p.set( FIRST_NAME,                  row[1] );
            p.set( STREET,                      row[2] );
            p.set( POSTCODE,                    row[3] == null ? 0 : Integer.valueOf( row[3] ));
            p.set( CITY,                        row[4] );
            p.set( BIRTHDATE, parseDate(        row[5] ));
            p.set( HEALTH_INSURENCE_IK,         row[6] == null ? 0 : Integer.valueOf( row[6] ));
            p.set( HEALTH_INSURENCE_NUMBER,     row[7] );
            p.set( PHONE,                       row[8] );
            p.set( BEFREIUNGSDATUM, parseDate(  row[9] ));
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            System.err.println( e.toString() + ": (" + p.getFullName() + ")" );
        }
        return p;
    }

    /**
     * Hilfsmethode zum sicheren Parsen von Datums-Strings.
     * @param dateStr
     * @return date as LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        if ( dateStr == null || dateStr.isEmpty() ) {
            return DEFAULT_DATE;
        }
        if ( dateStr.contains( "/" )) {
            dateStr = dateStr.replaceAll("/", ".");
        }
        if ( dateStr.contains( "," )) {
            dateStr = dateStr.replaceAll(",", ".");
        }
        try {
            if ( dateStr.matches( "\\d{4}\\-\\d{2}\\-\\d{2}" )) {
                return LocalDate.parse( dateStr, DateTimeFormatter.ofPattern( "yyyy-MM-dd" ));
            }
            if ( dateStr.matches( "\\d{2}\\.\\d{2}\\.\\d{4}" )) {
                return LocalDate.parse( dateStr, DateTimeFormatter.ofPattern( "dd.MM.yyyy" ));
            }
            if ( dateStr.matches( "\\d{2}\\.\\d{4}" )) {
                return LocalDate.parse( dateStr, DateTimeFormatter.ofPattern( "MM.yyyy" ));
            }
            if ( dateStr.matches( "\\d{2}\\.\\d{2}" )) {
                return LocalDate.parse( dateStr, DateTimeFormatter.ofPattern( "MM.yy" ));
            }
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            System.err.println( e.getMessage() );
            System.err.println( "Datumsangabe '" + e.getParsedString() + "' unverständlich" );
            return DEFAULT_DATE;
        }
    }

    public String getRawMengenList() {
        return itemQuantityList.stream()
                         .map(String::valueOf)
                         .collect( Collectors.joining( "," ));
    }

    public String getRawArtikelList() {
        return itemIdList.stream()
                         .map(String::valueOf)
                         .collect( Collectors.joining( "," ))
        ;
    }

    public void setArtikelIdList(String rawstr) {
        itemIdList = parseAsList(rawstr);
    }

    public void setMengenList(String rawstr) {
        itemQuantityList = parseAsList(rawstr);
    }

    private static ArrayList<Integer> parseAsList(String str) {
        ArrayList<Integer> list = new ArrayList<>();
        if ( str == null || str.isEmpty() ) {
            return list;
        }
        for ( String token : str.split( ",\\s*" )) {
            list.add( Integer.valueOf( token ));
        }
        return list;
    }

    /**
     * Hilfsmethode, um das PreparedStatement mit Werten aus dem Patient-Objekt zu füllen.
     * Mappt Java-Typen (LocalDate, String, etc.) auf SQL-Typen.
     */
    static int fillPreparedStatement(PreparedStatement pstmt, Patient p, List<PatientField> fields)
    throws SQLException {
        int i = 1;
        for (PatientField field : fields) {
            Object value = p.get(field);

            if (value == null) {
                pstmt.setNull(i, Types.NULL);
            }
            // Datumskonvertierung (LocalDate -> java.sql.Date)
            else if (value instanceof LocalDate) {
                pstmt.setDate( i, java.sql.Date.valueOf(( LocalDate ) value ));
            }
            // Patiententyp (Enum -> char/String Code)
            else if (field == PatientField.TYP) {
                pstmt.setString( i, String.valueOf( value ));
            }
            // Integer
            else if (value instanceof Integer) {
                pstmt.setInt( i, (Integer) value );
            }
            // Boolean
            else if (value instanceof Boolean) {
                pstmt.setBoolean( i, (Boolean) value );
            }
            // Fallback für alles andere (Strings)
            else {
                pstmt.setString( i, value.toString() );
            }
            ++i;
        }
        return i;
    }

    static void setStatementParam(PreparedStatement pstmt, int sqlIndex, Patient p, PatientField field)
    throws SQLException {
        Object value = p.get(field);
        if (value == null) {
            pstmt.setNull(sqlIndex, Types.NULL);
            return;
        }
        switch (field) {
            case BIRTHDATE:
            case RX_DATE:
            case FIRST_SUPPLY_DATE:
            case END_OF_LICENCE_DATE:
            case END_OF_BINDING_DATE:
            case BEFREIUNGSDATUM:
                LocalDate ld = (LocalDate) value;
                pstmt.setDate( sqlIndex, java.sql.Date.valueOf( ld ));
                break;
            case ID:
            case HEALTH_INSURENCE_IK:
                pstmt.setInt( sqlIndex, (Integer) value );
                break;
            case DELIVER:
            case PAUSE:
                pstmt.setBoolean( sqlIndex, (Boolean) value );
                break;
            default:
                pstmt.setString( sqlIndex, value.toString() );
                break;
        }
    }

    /**
     * Ersetzt alle Platzhalter in einem einzelnen String.
     * @param line Zeile
     * @param p Patient
     * @return Aufgelöster String
     * @throws java.io.IOException IOException, if conversion BufferedImage to base64-string fails
     */
    public static String replaceTemplate(String line, Patient p) throws IOException {
        if ( line == null || !line.contains( "⚕" )) {
            return line;
        }
        StringBuilder sb = new StringBuilder();
        Matcher matcher = TEMPLATE_PATTERN.matcher(line);
        int lastCursor = 0;

        // alle Zeilen durchgehen
        while ( matcher.find() ) {
            // Text vor dem Platzhalter anfügen
            sb.append( line, lastCursor, matcher.start() );
            // Den Platzhalter-Text extrahieren (z.B. "name")
            String tag = matcher.group(1);
            // Den passenden Wert aus dem Patienten-Objekt holen
            String replacement = getReplacementForTag(tag, p);
            // Wert einfügen (null-safe)
            sb.append( replacement != null ? escapeXml( replacement ) : "" );
            lastCursor = matcher.end();
        }
        // Rest der Zeile anfügen
        sb.append( line.substring( lastCursor ));
        return sb.toString();
    }

    /**
     * Sucht basierend auf dem Tag im Patient- oder SignatureField-Enum nach dem Wert.
     */
    private static String getReplacementForTag(String tag, Patient p) throws NoSuchElementException, IOException {
        // Feld suchen, dessen Template-String "⚕" + tag + "⚕" entspricht.
        Enum field = TAG_MAP.get(tag);
        if (field != null) {
            return p.getFormattedValue(field);
        } else {
            throw new NoSuchElementException("Tag \"" + tag + "\" unbekannt.");
        }
    }

    /**
     * Sonderzeichen für XML maskieren (z.B. & zu &amp;)
     */
    private static String escapeXml(String str) {
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }

    public void set(Enum field, Object value) throws NoSuchElementException {
        if (field == null || value == null) {
            return;
        }
        if (field instanceof PatientField) {
            PatientField pField = (PatientField) field;
            // wenn es bereits ein String ist, vorhandene Logik nutzen
            if (value instanceof String) {
                setValueAsString(pField, (String) value);
                return;
            }
            switch (pField) {
                case ID:                    id                          = ((Number)     value).intValue();  break;
                case POSTCODE:              postCode                    = ((Number)     value).intValue();  break;
                case BIRTHDATE:             birthDate                   = (LocalDate)   value;              break;
                case HEALTH_INSURENCE_IK:   healthInsurenceIK           = ((Number)     value).intValue();  break;
                case RX_DATE:               prescriptionDate            = (LocalDate)   value;              break;
                case FIRST_SUPPLY_DATE:     firstSupplyDate             = (LocalDate)   value;              break;
                case END_OF_LICENCE_DATE:   prescriptionExpiringDate    = (LocalDate)   value;              break;
                case END_OF_BINDING_DATE:   bindingExpiringDate         = (LocalDate)   value;              break;
                case BEFREIUNGSDATUM:       coPaymentFreeUntil          = (LocalDate)   value;              break;
                case TYP:                   type = InkoType.fromCode((    (Character)   value ));           break;
                case DELIVER:               deliver                     = (Boolean)     value;              break;
                case PAUSE:                 paused                      = (Boolean)     value;              break;
                default: throw new NoSuchElementException("Feld \"" + field + "\" unbekannt.");
            }
        } else if (field instanceof SignatureField) {
            if (value instanceof Signature) {
                setSignature((Signature) value);
                return;
            }
            SignatureField sField = (SignatureField) field;
            SignableDocument dType;
            switch (sField) {
                case SIGN_BERATUNG:     dType = BERATUNG;   break;
                case SIGN_BINDUNG:      dType = BINDUNG;    break;
                case SIGN_MEHRKOSTEN:   dType = MEHRKOSTEN; break;
                case DATE_BERATUNG:     dType = BERATUNG;   break;
                case DATE_BINDUNG:      dType = BINDUNG;    break;
                case DATE_MEHRKOSTEN:   dType = MEHRKOSTEN; break;
                default: throw new NoSuchElementException("Feld \"" + field + "\" unbekannt.");
            }
            if (value instanceof BufferedImage) {
                if ( signMap.containsKey( dType )) {
                    signMap.get(dType).setSign((BufferedImage) value);
                } else {
                    signMap.put( dType, new Signature( dType, (BufferedImage) value, LocalDate.now() ));
                }
            } else if (value instanceof LocalDate) {
                if ( signMap.containsKey( dType )) {
                    signMap.get(dType).setDate((LocalDate) value);
                } else {
                    signMap.put( dType, new Signature( dType, null, (LocalDate) value ));
                }
            } else if (value instanceof String) {
                if ( signMap.containsKey( dType )) {
                    signMap.get(dType).setDate( LocalDate.parse((CharSequence) value ));
                } else {
                    signMap.put( dType, new Signature( dType, null, LocalDate.parse((CharSequence) value )));
                }
            } else {
                throw new NoSuchElementException("Wert vom Typ \"" + value.getClass() + "\" nicht unterstützt.");
            }
        } else {
            throw new NoSuchElementException("Aufzählung/Feld vom Typ \"" + field.getClass() + "\" nicht unterstützt.");
        }
        setModified(true);
    }

    public void setValueAsString(PatientField field, String value) throws
        NoSuchElementException, NumberFormatException, DateTimeParseException {
        if (value == null) {
            return;
        }
        switch (field) {
            case ID:                        id                          = Integer.parseInt(value);               break;
            case LAST_NAME:                 lastName                    = value;                                 break;
            case FIRST_NAME:                firstName                   = value;                                 break;
            case STREET:                    street                      = value;                                 break;
            case POSTCODE:                  postCode                    = Integer.valueOf(value);                break;
            case CITY:                      city                        = value;                                 break;
            case BIRTHDATE:                 birthDate                   = LocalDate.parse(value);                break;
            case HEALTH_INSURENCE_IK:       healthInsurenceIK           = Integer.valueOf(value);                break;
            case HEALTH_INSURENCE_NUMBER:   insurenceNumber             = value;                                 break;
            case PHONE:                     phoneNumber                 = value;                                 break;
            case COMMENT:                   comment                     = value;                                 break;
            case RX_DATE:                   prescriptionDate            = LocalDate.parse(value);                break;
            case FIRST_SUPPLY_DATE:         firstSupplyDate             = LocalDate.parse(value);                break;
            case END_OF_LICENCE_DATE:       prescriptionExpiringDate    = LocalDate.parse(value);                break;
            case END_OF_BINDING_DATE:       bindingExpiringDate         = LocalDate.parse(value);                break;
            case DELIVER:                   deliver                     = Boolean.parseBoolean(value);           break;
            case PAUSE:                     paused                      = Boolean.parseBoolean(value);           break;
            case BEFREIUNGSDATUM:           coPaymentFreeUntil          = LocalDate.parse(value);                break;
            case TYP:                       type                        = InkoType.fromCode( value.charAt( 0 )); break;
            case MENGENLISTE:               setMengenList(value);                                                break;
            case ARTIKELLISTE:              setArtikelIdList(value);                                             break;
            default: throw new NoSuchElementException("Feld \"" + field + "\" unbekannt.");
        }
        setModified(true);
    }

    public String getFormattedValue(Enum field) throws NoSuchElementException, IOException {
        Object obj = get(field);
        if (obj == null) {
            throw new NoSuchElementException("getFormattedValue: get(Enum field) liefert null.");
        }
        if (field instanceof PatientField) {
            switch ((PatientField) field) {
                case BEFREIUNGSDATUM:
                    if ( isCoPaymentFree() ) {
                        return "frei bis " + ((ChronoLocalDate) obj).format(DEFAULT_FORMATTER);
                    } else {
                        return "zuzahlungspflichtig";
                    }
            }
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof LocalDate) {
            return ((ChronoLocalDate) obj).format(DEFAULT_FORMATTER);
        }
        if (obj instanceof BufferedImage) {
            return convertToHtmlBase64((BufferedImage) obj);
        }
        return obj.toString();
    }

    public String getValue(Enum field) throws NoSuchElementException {
        return get(field).toString();
    }

    public Object get(Enum field) throws NoSuchElementException {
        if (field instanceof PatientField) {
            switch ((PatientField) field) {
                case ID:                        return id;
                case LAST_NAME:                 return lastName;
                case FIRST_NAME:                return firstName;
                case STREET:                    return street;
                case POSTCODE:                  return postCode;
                case CITY:                      return city;
                case BIRTHDATE:                 return birthDate;
                case HEALTH_INSURENCE_IK:       return healthInsurenceIK;
                case HEALTH_INSURENCE_NUMBER:   return insurenceNumber;
                case PHONE:                     return phoneNumber;
                case COMMENT:                   return comment;
                case RX_DATE:                   return prescriptionDate;
                case FIRST_SUPPLY_DATE:         return firstSupplyDate;
                case END_OF_LICENCE_DATE:       return prescriptionExpiringDate;
                case END_OF_BINDING_DATE:       return bindingExpiringDate;
                case DELIVER:                   return deliver;
                case PAUSE:                     return paused;
                case BEFREIUNGSDATUM:           return coPaymentFreeUntil;
                case TYP:                       return type.getCode();
                case MENGENLISTE:               return getRawMengenList();
                case ARTIKELLISTE:              return getRawArtikelList();
                case BESONDERHEITEN:            return getBesonderheiten();
                case ACTK:                      return getACTK();
                case KK_NAME:                   return getHealthInsurer();
                case TYPE_LABEL:                return type.getLabel();
                case HIMI:                      return getArtikelListAsString();
            }
        }
        if (field instanceof SignatureField) {
            switch ((SignatureField) field) {
                case SIGN_BERATUNG:
                    return getSignature(BERATUNG)   != null ? getSignature(BERATUNG).getSign()   : "";
                case DATE_BERATUNG:
                    return getSignature(BERATUNG)   != null ? getSignature(BERATUNG).getDate()   : LocalDate.now();
                case SIGN_BINDUNG:
                    return getSignature(BINDUNG)    != null ? getSignature(BINDUNG).getSign()    : "";
                case DATE_BINDUNG:
                    return getSignature(BINDUNG)    != null ? getSignature(BINDUNG).getDate()    : LocalDate.now();
                case SIGN_MEHRKOSTEN:
                    return getSignature(MEHRKOSTEN) != null ? getSignature(MEHRKOSTEN).getSign() : "";
                case DATE_MEHRKOSTEN:
                    return getSignature(MEHRKOSTEN) != null ? getSignature(MEHRKOSTEN).getDate() : LocalDate.now();
            }
        }
        throw new NoSuchElementException("Aufzählung/Feld \"" + field + "\" nicht unterstützt.");
    }

}

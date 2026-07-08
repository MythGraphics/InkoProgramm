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

import java.awt.image.BufferedImage;
import java.time.LocalDate;

public enum SignatureField {

    PATIENT_ID("p_id", "int unsigned not null unique", Integer.class, null),
    SIGN_BERATUNG("BERATUNG_Unterschrift", "mediumblob", BufferedImage.class, "⚕sign_beratung⚕"),
    DATE_BERATUNG("BERATUNG_Datum", "date", LocalDate.class, "⚕date_beratung⚕"),
    SIGN_BINDUNG("BINDUNG_Unterschrift", "mediumblob", BufferedImage.class, "⚕sign_bindung⚕"),
    DATE_BINDUNG("BINDUNG_Datum", "date", LocalDate.class, "⚕date_bindung⚕"),
    SIGN_MEHRKOSTEN("MEHRKOSTEN_Unterschrift", "mediumblob", BufferedImage.class, "⚕sign_mehrkosten⚕"),
    DATE_MEHRKOSTEN("MEHRKOSTEN_Datum", "date", LocalDate.class, "⚕date_mehrkosten⚕");

    private final String dbName;
    private final String dbType;
    private final Class<?> dataType;
    private final String template;

    SignatureField(String dbName, String dbType, Class<?> dataType, String template) {
        this.dbName     = dbName;
        this.dbType     = dbType;
        this.dataType   = dataType;
        this.template   = template;
    }

    public String getDBName() {
        return dbName;
    }

    public String getDBType() {
        return dbType;
    }

    public Class<?> getType() {
        return dataType;
    }

    public String getTemplate() {
        return template;
    }

}

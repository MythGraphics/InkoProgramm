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

import static inko.SignatureField.*;

public enum SignableDocument {

    BERATUNG    (SIGN_BERATUNG,     DATE_BERATUNG),
    BINDUNG     (SIGN_BINDUNG,      DATE_BINDUNG),
    MEHRKOSTEN  (SIGN_MEHRKOSTEN,   DATE_MEHRKOSTEN);

    private final SignatureField signField;
    private final SignatureField dateField;

    SignableDocument(SignatureField signField, SignatureField dateField) {
        this.signField = signField;
        this.dateField = dateField;
    }

    public SignatureField getSignField() {
        return signField;
    }

    public SignatureField getDateField() {
        return dateField;
    }

}

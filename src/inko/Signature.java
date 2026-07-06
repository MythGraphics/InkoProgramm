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

import static inko.ImageUtility.convertToBase64String;
import static inko.ImageUtility.convertToHtmlBase64;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

public class Signature {

    private final SignableDocument documentType;

    private BufferedImage sign;
    private LocalDate date;
    private boolean modified = false;

    public Signature(SignableDocument documentType, BufferedImage sign) {
        this( documentType, sign, LocalDate.now() );
    }

    public Signature(SignableDocument documentType, BufferedImage sign, LocalDate date) {
        this.documentType = documentType;
        this.sign = sign;
        this.date = date == null ? LocalDate.now() : date;
    }

    public Signature(SignableDocument documentType, BufferedImage sign, Date date) {
        this.documentType = documentType;
        this.sign = sign;
        this.date = date == null ? LocalDate.now() : date.toLocalDate();
    }

    public SignableDocument getDocumentType() {
        return documentType;
    }

    public BufferedImage getSign() {
        return sign;
    }

    public String getBase64HtmlSign() throws IOException {
        return convertToHtmlBase64(sign);
    }

    public String getBase64Sign() throws IOException {
        return convertToBase64String(sign);
    }

    public void setSign(BufferedImage sign) {
        this.sign = sign;
        modified = true;
    }

    public void setDate(LocalDate date) {
        this.date = date;
        modified = true;
    }

    public void setDate(Date date) {
        this.date = date.toLocalDate();
        modified = true;
    }

    public void setSign(BufferedImage sign, LocalDate date) {
        setSign(sign);
        setDate(date);
        modified = true;
    }

    public LocalDate getDate() {
        return date == null ? LocalDate.now() : date;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isModified() {
        return modified;
    }

    @Override
    public String toString() {
        String imgString;
        try {
            imgString = getBase64Sign();
        } catch (IOException e) {
            imgString = e.getMessage();
        }
        return documentType + ": " + date + "\n" +
               "Unterschrift (Base64): " + imgString + "\n" +
               "modified: " + modified;
    }

}

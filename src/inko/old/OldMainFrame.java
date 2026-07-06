/*
 *
 */

package inko.old;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 1.1.0
 *
 */

import inko.Artikel;
import inko.Document;
import static inko.Document.*;
import inko.HTMLMaker;
import static inko.SignableDocument.BERATUNG;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;

public class OldMainFrame extends inko.MainFrame {

    private final PatientArtikelListModel patientArtikelListModel;

    private JTextField artikelTextField;

    public OldMainFrame() {
        patientArtikelListModel = new PatientArtikelListModel(( inko.old.DBio ) pio );
        initComponents();
    }

    public static void main(String args[]) {
        showGUI(new OldMainFrame() );
    }

    @Override
    protected void loadEntry(inko.Patient patient) {
        super.loadEntry(patient);
        if ( patientArtikelListModel == null || patient == null || patient.getId() == -1 ) {
            return;
        }

        patientArtikelListModel.setPatient( patient.getId() );
        loadEntryUpdateUI(patient);
        patientTableModel.getPatient().setModified(false);
    }

    @Override
    protected inko.DBio getDBio() {
        return new DBio( server, inko.DBio.DEFAULT_PORT, new net.Login( user, pass ));
    }

    @Override
    protected void jArtikelComboBoxEditorKeyReleased(KeyEvent evt) {
        // noop -> den Listener brauchen wir hier nicht
    }

    @Override
    protected void _jAddArtikelButtonActionPerformed(ActionEvent evt) {
        patientArtikelListModel.addElement( Artikel.parseArtikelString( artikelTextField.getText() ));
        patientTableModel.getPatient().setModified(true);
    }

    @Override
    protected void _jRemoveArtikelButtonActionPerformed(ActionEvent evt) {
        int index = jPatientArtikelList.getSelectedIndex();
        if (index > -1) {
            patientArtikelListModel.remove(index);
            patientTableModel.getPatient().setModified(true);
        }
    }

    @Override
    protected void _jChangeArtikelButtonActionPerformed(ActionEvent evt) {
        int index = jPatientArtikelList.getSelectedIndex();
        if (index == -1) {
            return;
        }
        patientTableModel.getPatient().setModified(true);
        patientArtikelListModel.set(index, Artikel.parseArtikelString( artikelTextField.getText() ));
    }

    @Override
    protected void jPatientArtikelListMouseClicked(MouseEvent evt) {
        Artikel artikel = jPatientArtikelList.getSelectedValue();
        if (artikel == null) {
            return;
        }
        artikelTextField.setText( artikel.getFullArtikelString() );
    }

    @Override
    protected void makeHtml(ActionEvent evt, Document doc) {
        HTMLMaker htmlmaker = new HTMLMaker( doc.getFileName(), outpath+doc.getFileName() );
        try {
            switch (doc) {
                case HIMILISTE:
                    List<Patient> patientList = ((inko.old.DBio) pio).getOldPatientList();
                    List<Artikel> artikelList = new ArrayList<>();
                    for ( Patient p : patientList ) {
                        for ( Artikel a : p.getArtikelList() ) {
                            artikelList.add(a);
                        }
                    }
                    htmlmaker.makeArtikelList(patientList, artikelList);
                    break;
                case HIMILISTE2:
                    List<Patient> himilist = ((inko.old.DBio) pio).getOldPatientList();
                    htmlmaker.makeArtikelList2(himilist);
                    break;
                case DECKBLATT:
                    Patient p1 = Patient.fromPatient( patientTableModel.getPatient(), patientArtikelListModel );
                    htmlmaker.makeFrontpage(p1);
                    break;
                case BERATUNGSBOGEN:
                    Patient p2 = Patient.fromPatient( patientTableModel.getPatient(), patientArtikelListModel );
                    htmlmaker.makeSignedDocument(p2, BERATUNG);
                    break;
                case LIEFERLISTE:
                    List<Patient> lieferList = ((inko.old.DBio) pio).getOldPatientList();
                    htmlmaker.makeDeliveryList(lieferList);
                    break;
                default:
                    super.makeHtml(evt, doc);
            }
        } catch (IOException e) {
            statusField.showError(HTMLMaker.IOERROR);
            e.printStackTrace();
        }
    }

    @Override
    protected void saveEntry(inko.Patient p) {
        super.saveEntry( Patient.fromPatient( p, patientArtikelListModel ));
        patientTableModel.getPatient().setModified(false);
    }

    @Override
    protected JComboBox<Artikel> getArtikelComboBox() {
        return new JComboBox<Artikel>() {
            @Override
            public void updateUI() {
                setUI( new javax.swing.plaf.basic.BasicComboBoxUI() {
                    @Override
                    protected javax.swing.plaf.basic.ComboPopup createPopup() {
                        return new javax.swing.plaf.basic.BasicComboPopup(comboBox) {
                            @Override
                            public void show() {
                                // Popup nie anzeigen
                            }
        };}});}};
    }

    private void initComponents() {
        jPatientArtikelList.setModel(patientArtikelListModel);
        artikelTextField = (JTextField) jArtikelComboBox.getEditor().getEditorComponent();
        for ( Component comp : jArtikelComboBox.getComponents() ) {
            if (comp instanceof JButton) {
                comp.setEnabled(false);
            }
        }

        jSpinner1.setEnabled(false);
        jCreateArtikelButton.setEnabled(false);
        jUpButton.setEnabled(false);
        jDownButton.setEnabled(false);

        loadEntry( super.patientTableModel.getPatient() );
    }

}


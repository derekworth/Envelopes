package server.local;

import model.tables.EnvelopesTableModel;
import model.tables.AccountsTableModel;
import model.tables.TransactionsTableModel;
import model.tables.EmailTableModel;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import misc.Utilities;
import model.ModelController;
import server.remote.GmailCommunicator;

/**
 *
 * @author Worth
 */
public class Console extends javax.swing.JFrame {
    
    private static final String VER = "2018-03-11";

    private final Console thisConsole = this;
    private final String TITLE = "Envelopes";
    private LinkedList<String> errorMsg = new LinkedList();
    private LinkedList<String> cmdHistory = new LinkedList();
    private int consoleLoginFailCount = 0;
    private int serverLoginFailCount = 0;
    private boolean serverIsOn;
    ExecutorService exec;
    private String currUser;

    private static final String ALL = "-all-";
    private static final String NONE = "-none-";
    private static final String UNCAT = "uncategorized";
    private static final int DEFAULT_TRANS_COUNT = 250;

    Runnable gmailServer;

    private ModelController mc;
    private GmailCommunicator gc;
    private EnvelopesTableModel envelopesTM;
    private AccountsTableModel accountsTM;
    private TransactionsTableModel transactionsTM;
    private EmailTableModel emailTM;
    
    private ServerSocket socket;

    public Console() {
        checkForLatestVersion();
        
        gmailServer = new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                int count = 1;
                while (serverIsOn) {
                    try {
                        if (gc.receive()) {
                            updateAll();
                        }
                        TimeUnit.SECONDS.sleep(6);
                        gmailServerStatus.setText("run-time ~ " + Utilities.getDuration((System.currentTimeMillis() - start) / 1000));
                    } catch (InterruptedException ex) {
                    }
                }
            }
        };
        
        try {
            // prevents multiple instances of console
            socket = new ServerSocket(61234);
            // initialize all GUI components
            initComponents();
            
            // initialize model controller
            mc = new ModelController();
            // establish Gmail communicator
            gc = new GmailCommunicator(mc);
            currUser = "";
            // initialize tables with model controller
            envelopesTM = new EnvelopesTableModel(mc);
            accountsTM = new AccountsTableModel(mc);
            transactionsTM = new TransactionsTableModel(mc);
            emailTM = new EmailTableModel(mc);
            // initialized envelopes table
            envelopesTable.setModel(envelopesTM);
            envelopesTable.getColumnModel().getColumn(0).setCellRenderer(envelopesTM.getBoldRenderer());
            envelopesTable.getColumnModel().getColumn(1).setCellRenderer(envelopesTM.getBoldRenderer());
            envelopesTable.getColumnModel().getColumn(1).setPreferredWidth(80);
            // initialize accounts table
            accountsTable.setModel(accountsTM);
            accountsTable.getColumnModel().getColumn(0).setCellRenderer(accountsTM.getBoldRenderer());
            accountsTable.getColumnModel().getColumn(1).setCellRenderer(accountsTM.getBoldRenderer());
            accountsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
            // initialize transactions table
            transactionsTable.setModel(transactionsTM);
            transactionsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
            transactionsTable.getColumnModel().getColumn(0).setMaxWidth(100);
            transactionsTable.getColumnModel().getColumn(1).setPreferredWidth(240);
            transactionsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
            transactionsTable.getColumnModel().getColumn(2).setMaxWidth(120);
            transactionsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
            transactionsTable.getColumnModel().getColumn(3).setMaxWidth(120);
            transactionsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
            transactionsTable.getColumnModel().getColumn(4).setMaxWidth(120);
            for (int i = 0; i < transactionsTable.getColumnCount(); i++) {
                transactionsTable.getColumnModel().getColumn(i).setCellRenderer(transactionsTM.getRenderer());
            }
            // initialize email table
            emailTable.setModel(emailTM);
            emailTable.getColumnModel().getColumn(0).setMaxWidth(180);
            emailTable.getColumnModel().getColumn(0).setPreferredWidth(180);
            emailTable.getColumnModel().getColumn(1).setMaxWidth(180);
            emailTable.getColumnModel().getColumn(1).setPreferredWidth(180);
            emailTable.getColumnModel().getColumn(2).setMaxWidth(60);
            emailTable.getColumnModel().getColumn(2).setPreferredWidth(60);
            emailTable.getColumnModel().getColumn(3).setMaxWidth(150);
            emailTable.getColumnModel().getColumn(3).setPreferredWidth(80);
            for (int i = 0; i < emailTable.getColumnCount(); i++) {
                emailTable.getColumnModel().getColumn(i).setCellRenderer(emailTM.getRenderer());
            }
            // set gmail credentials
            exec = Executors.newSingleThreadExecutor();
            if (gc.isValidCredentials(mc.getGmailUsername(), mc.getGmailPassword())) {
                gmailPassword.setText(mc.getGmailPassword());
                gmailUsername.setText(mc.getGmailUsername());
                // starts Gmail Server automatically on startup
                serverToggleButton.setSelected(true);
                // retrieve username and password from the text fields
                serverIsOn = true;
                exec.submit(gmailServer);
                serverToggleButton.setText("Stop Server");
                gmailServerStatus.setText("Gmail server is now ON.");
                serverLoginFailCount = 0;
                gmailPassword.setEnabled(false);
                gmailUsername.setEnabled(false);
            }
            // populate transaction date fields
            transFromField.setText(Utilities.getDatestamp(-28));
            transToField.setText(Utilities.getDatestamp(0));
            transactionDateField.setText(Utilities.getDatestamp(0));
            // add interval options
            intervalTypeDropdown.removeAllItems();
            intervalTypeDropdown.addItem("monthly");
            intervalTypeDropdown.addItem("weekly");
            intervalTypeDropdown.addItem("daily");
            // update
            selectedAcctAmtLabel.setText(mc.getAccountAmount(ALL, ALL));
            selectedEnvAmtLabel.setText(mc.getEnvelopeAmount(ALL, ALL));
            // update all dropdowns and tables
            updateAll();
            // set window title
            setTitle(TITLE);
            // disable all login components 
            enabledLoginComponents(false);
        } catch (IOException ex) {
            System.exit(0);
        }
    }
    
    private void checkForLatestVersion() {
        try {
            URL oracle = new URL("https://github.com/derekworth/Famliy-Envelopes/blob/master/README.md");
            try (BufferedReader in = new BufferedReader( new InputStreamReader(oracle.openStream()) )) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if(inputLine.contains("Last Updated")) {
                        if(inputLine.contains(VER)) {
                            return;
                        }
                    }
                }
                // pop-up indicating you should pull latest update
                String msg = "A newer version of Envelopes is available.\n"
                           + "Would you like to update now?";
                String title = "Software Update";
                int yes = 0;
                int opt = JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt == yes) {
                    saveUrl("Envelopes.jar","https://github.com/derekworth/Famliy-Envelopes/blob/master/Envelopes.jar?raw=true");
                    System.exit(0);
                }
                
            } catch (IOException ex) { /* DO NOTHING */ }
        } catch (MalformedURLException ex) { /* DO NOTHING */ }
    }
    
    private void saveUrl(final String filename, final String urlString) {
        try {
            FileOutputStream fout;
            try (BufferedInputStream in = new BufferedInputStream(new URL(urlString).openStream())) {
                fout = new FileOutputStream(filename);
                final byte data[] = new byte[1024];
                int count;
                while ((count = in.read(data, 0, 1024)) != -1) {
                    fout.write(data, 0, count);
                }
            }
            fout.close();
            JOptionPane.showMessageDialog(this, "Update successful!!!\n\n"
                                              + "NOTE: You must re-open Envelopes.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "ERROR: update failed. Please check Internet\n"
                                              + "connection and try again.");
        }
    }

    public final void updateAll() {
        updateAllTables();
        updateAllDropdowns();
    }

    /**
     * **UPDATE TABLES***
     */
    public final void updateAllTables() {
        updateTransactionTable();
        updateAccountTable();
        updateEnvelopeTable();
        updateEmailTable();
    }

    public final void updateAccountTable() {
        selectedAcctAmtLabel.setText(mc.getAccountAmount((String) transAccountDropdown.getSelectedItem(), ALL));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                accountsTable.updateUI();
            }
        });
    }

    public final void updateEnvelopeTable() {
        selectedEnvAmtLabel.setText(mc.getEnvelopeAmount((String) transEnvelopeDropdown.getSelectedItem(), ALL));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                envelopesTable.updateUI();
            }
        });
    }

    public final void updateTransactionTable() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                transactionsTable.updateUI();
            }
        });
    }

    public final void updateEmailTable() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                emailTable.updateUI();
            }
        });
    }

    public final void allowEditing(boolean isAllowed) {
        accountsTM.setEditing(isAllowed);
        envelopesTM.setEditing(isAllowed);
        transactionsTM.setEditing(isAllowed);
    }

    /**
     * **UPDATE DROPDOWNS***
     */
    public final void updateAllDropdowns() {
        updateAccountDropdowns();
        updateCategoryDropdowns();
        updateEnvelopeDropdowns();
        updateUserDropdowns();
        updateEmailDropdown();
    }

    public final void updateAccountDropdowns() {
        // reset account dropdown lists
        transAccountDropdown.removeAllItems();
        transAccountDropdown.addItem(ALL);
        newTransAcctDropdown.removeAllItems();
        acctTransferFrom.removeAllItems();
        acctTransferTo.removeAllItems();
        // populates account dropdown lists
        for (String name : mc.getAccountNames()) {
            transAccountDropdown.addItem(name);
            newTransAcctDropdown.addItem(name);
            acctTransferFrom.addItem(name);
            acctTransferTo.addItem(name);
        }
    }

    public final void updateEnvelopeDropdowns() {
        // reset envelope dropdowns
        transEnvelopeDropdown.removeAllItems();
        newTransEnvDropdown.removeAllItems();
        mergeEnvelopesList.removeAllItems();
        envTransferFrom.removeAllItems();
        envTransferTo.removeAllItems();
        // poplates envelope dropdowns
        transEnvelopeDropdown.addItem(ALL);
        for (String name : mc.getEnvelopeNames()) {
            newTransEnvDropdown.addItem(name);
            mergeEnvelopesList.addItem(name);
            envTransferFrom.addItem(name);
            envTransferTo.addItem(name);
            transEnvelopeDropdown.addItem(name);
        }
    }

    public final void updateCategoryDropdowns() {
        // reset category dropdown
        transCategoryDropdown.removeAllItems();
        // populate
        transCategoryDropdown.addItem(NONE);
        for (String name : mc.getCategoryNames()) {
            transCategoryDropdown.addItem(name);
        }
    }

    public final void updateUserDropdowns() {
        // reset user dropdowns
        loginUserDropdown.removeAllItems();
        removeUserDropdown.removeAllItems();
        updateUserDropdown.removeAllItems();
        emailUserDropdown.removeAllItems();
        // populate dropdowns
        for (String un : mc.getUsernames()) {
            // add all users to login list
            loginUserDropdown.addItem(un);
            // add all users to email list
            emailUserDropdown.addItem(un);
            if (currUser.length() > 0) { // logged in
                if (mc.isUserAdmin(currUser)) { // user is the admin
                    // add all users to update list
                    updateUserDropdown.addItem(un);
                    // add all other users to remove list
                    if (!currUser.equalsIgnoreCase(un)) {
                        removeUserDropdown.addItem(un);
                    }
                } else { // user is a regular user
                    if (currUser.equalsIgnoreCase(un)) { // logged in as regular user
                        // add only current user to update list
                        updateUserDropdown.addItem(un);
                    } else {
                        // adds non-admin users that are not the current user
                        if (!mc.isUserAdmin(un)) {
                            removeUserDropdown.addItem(un);
                        }
                    }
                }
            }
        }
    }

    public final void updateEmailDropdown() {
        // reset dropdown
        emailAddressDropdown.removeAllItems();
        for (String addr : mc.getEmailAddresses()) {
            emailAddressDropdown.addItem(addr);
        }
    }

    private void updateSelected() {
        String acctName = (String) transAccountDropdown.getSelectedItem();
        String envName = (String) transEnvelopeDropdown.getSelectedItem();
        boolean hideTx = hideTransfersToggleButton.isSelected();
        if (dateRangeCheckBox.isSelected()) {
            String from = transFromField.getText();
            String to = transToField.getText();
            mc.showTransactionsByDateRange(acctName, envName, from, to, hideTx);
        } else {
            int to;
            try {
                to = Integer.parseInt(transactionQtyTextField.getText());
                if (to < 0) {
                    to = 0;
                }
            } catch (NumberFormatException e) {
                to = DEFAULT_TRANS_COUNT;
            }
            mc.showTransactionsByIndexRange(acctName, envName, 1, to, hideTx);
        }
        selectedAcctAmtLabel.setText(mc.getAccountAmount(acctName, ALL));
        selectedEnvAmtLabel.setText(mc.getEnvelopeAmount(envName, ALL));
        updateTransactionTable();
    }

    /**
     * **MISC***
     */
    public final void validateTransactionFields() {
        String dateError = "date must be in the format yyyy-mm-dd";

        boolean error1 = false, error2 = false, error3 = false;

        // validate from date
        if (transFromField.getText().length() > 0) {
            String fromDate = Utilities.validateDate(transFromField.getText());
            if (fromDate.length() == 10) {
                transFromField.setText(fromDate);
                fromLabel.setForeground(Color.BLACK);
                fromLabel.setText("From:");
            } else {
                fromLabel.setForeground(Color.RED);
                fromLabel.setText("*From:");
                error1 = true;
            }
        }

        // validate to date
        if (transToField.getText().length() > 0) {
            String toDate = Utilities.validateDate(transToField.getText());
            if (toDate.length() == 10) {
                transToField.setText(toDate);
                toLabel.setForeground(Color.BLACK);
                toLabel.setText("To:");
            } else {
                toLabel.setForeground(Color.RED);
                toLabel.setText("*To:");
                error2 = true;
            }
        }

        // validate new transaction date
        if (transactionDateField.getText().length() > 0) {
            String transDate = Utilities.validateDate(transactionDateField.getText());
            if (transDate.length() == 10) {
                transactionDateField.setText(transDate);
                dateLabel.setForeground(Color.BLACK);
                dateLabel.setText("Date:");
            } else {
                dateLabel.setForeground(Color.RED);
                dateLabel.setText("*Date:");
                error3 = true;
            }
        }

        if (error1 || error2 || error3) {
            addErrorMsg(dateError);
        } else {
            removeErrorMsg(dateError);
        }
    }

    public final void addErrorMsg(String msg) {
        // add error message if it does not already exist
        if (!errorMsg.contains(msg)) {
            errorMsg.add(msg);
            String fullMsg = "ERROR: ";
            // builds new message
            for (String m : errorMsg) {
                fullMsg += m + "; ";
            }
            // removes last "new line"
            fullMsg = fullMsg.substring(0, fullMsg.length() - 2);
            // sets new message
            message.setText(fullMsg);
        }
    }

    public final void removeErrorMsg(String msg) {
        // removes error message if it exist
        if (errorMsg.contains(msg)) {
            errorMsg.remove(msg);
            String fullMsg = "ERROR: ";
            if (errorMsg.isEmpty()) {
                fullMsg = "";
            }

            // builds new message
            for (String m : errorMsg) {
                fullMsg += m + '\n';
            }
            // removes last "new line"
            if (fullMsg.length() < 0) {
                fullMsg = fullMsg.substring(0, fullMsg.length() - 1);
            }
            // sets new message
            message.setText(fullMsg);
        }
    }

    public final void enabledLoginComponents(boolean isLoggedIn) {
        // update buttons
        addAccountButton.setEnabled(isLoggedIn);
        removeAccountButton.setEnabled(isLoggedIn);
        addEnvelopeButton.setEnabled(isLoggedIn);
        removeEnvelopeButton.setEnabled(isLoggedIn);
        mergeEnvelopesButton.setEnabled(isLoggedIn);
        addCategoryButton.setEnabled(isLoggedIn);
        removeCategoryButton.setEnabled(isLoggedIn);
        setCategoryButton.setEnabled(isLoggedIn);
        newTransactionButton.setEnabled(isLoggedIn);
        addUserButton.setEnabled(isLoggedIn);
        updateUserButton.setEnabled(isLoggedIn);
        removeUserButton.setEnabled(isLoggedIn);
        acctTransferButton.setEnabled(isLoggedIn);
        envTransferButton.setEnabled(isLoggedIn);
        allowEmail.setEnabled(isLoggedIn);
        blockEmail.setEnabled(isLoggedIn);
        serverToggleButton.setEnabled(!isLoggedIn);

        // fields
        userPassword.setEnabled(!isLoggedIn);
        addUserTextField.setEnabled(isLoggedIn);
        addUserPasswordField.setEnabled(isLoggedIn);
        updateUserTextField.setEnabled(isLoggedIn);
        updateUserPasswordField.setEnabled(isLoggedIn);
        newAccountField.setEnabled(isLoggedIn);
        newEnvelopeField.setEnabled(isLoggedIn);
        newCategoryField.setEnabled(isLoggedIn);
        transactionDateField.setEnabled(isLoggedIn);
        transactionDescriptionField.setEnabled(isLoggedIn);
        transactionAmtField.setEnabled(isLoggedIn);

        // dropdowns
        loginUserDropdown.setEnabled(!isLoggedIn);
        mergeEnvelopesList.setEnabled(isLoggedIn);
        transCategoryDropdown.setEnabled(isLoggedIn);
        newTransAcctDropdown.setEnabled(isLoggedIn);
        newTransEnvDropdown.setEnabled(isLoggedIn);
        updateUserDropdown.setEnabled(isLoggedIn);
        removeUserDropdown.setEnabled(isLoggedIn);
        acctTransferFrom.setEnabled(isLoggedIn);
        acctTransferTo.setEnabled(isLoggedIn);
        envTransferFrom.setEnabled(isLoggedIn);
        envTransferTo.setEnabled(isLoggedIn);
        emailAddressDropdown.setEnabled(isLoggedIn);
        emailUserDropdown.setEnabled(isLoggedIn);

        // table editing
        allowEditing(isLoggedIn);

        // admin option in File dropdown menu
        if (currUser.length() > 0 && mc.isUserAdmin(currUser)) {
            resetDatabaseMenuItem.setEnabled(isLoggedIn);
            resetDatabaseMenuItem.setVisible(isLoggedIn);
            jSeparator3.setVisible(isLoggedIn);
        } else {
            resetDatabaseMenuItem.setEnabled(false);
            resetDatabaseMenuItem.setVisible(false);
            jSeparator3.setVisible(false);
        }
    }
    
    public final void attemptLogin() {
        String un = loginUserDropdown.getSelectedItem().toString();
        String pw = "";
        for (char c : userPassword.getPassword()) {
            pw += c;
        }
        if (mc.getPassword(un).equals(Utilities.getHash(pw))) { // successful login
            transactionDateField.setText(Utilities.getDatestamp(0));
            currUser = un;
            enabledLoginComponents(true);
            consoleLoginFailCount = 0;
            userPassword.setText("");
            loginToggleButton.setSelected(true);
            loginToggleButton.setText("Sign Out");
            loginStatus.setText("Welcome " + currUser + "! You are now signed in.");
            updateUserDropdowns();
            // shutdown server while logged in
            shutdownServer();
        } else { // failed login
            // set sign in settings
            loginToggleButton.setSelected(false);
            userPassword.setText("");
            loginStatus.setText("Error(" + ++consoleLoginFailCount + "): incorrect password.");
        }
    }
    
    public final void logout() {
        currUser = "";
        enabledLoginComponents(false);
        // set sign in settings
        loginToggleButton.setText("Sign In");
        loginToggleButton.setSelected(false);
        loginStatus.setText("You are now signed out.");
        cmdHistory.clear();
        updateUserDropdowns();
        // attempt to start server while logged out
        attemptStartServer();
    }
    
    public final void attemptStartServer() {
        // retrieve username and password from the text fields
        String gmailUN, gmailPW = "";
        gmailUN = gmailUsername.getText();
        for (char c : gmailPassword.getPassword()) {
            gmailPW += c;
        }
        // check if un & pw are valid
        if (gc.isValidCredentials(gmailUN, gmailPW)) {
            // update model
            mc.setGmailUsername(gmailUN);
            mc.setGmailPassword(gmailPW);
            // update view
            serverToggleButton.setText("Stop Server");
            gmailServerStatus.setText("Gmail server is now ON.");
            serverLoginFailCount = 0;
            gmailUsername.setEnabled(false);
            gmailPassword.setEnabled(false);
            // turn on server
            serverIsOn = true;
            exec = Executors.newSingleThreadExecutor();
            exec.submit(gmailServer);
        } else {
            serverToggleButton.setSelected(false);
            gmailServerStatus.setText("Error(" + ++serverLoginFailCount + "): invalid username and/or password.");
        }
    }
    
    public final void shutdownServer() {
        // update view
        serverToggleButton.setText("Start Server");
        serverToggleButton.setSelected(false);
        gmailServerStatus.setText("Gmail server is now OFF.");
        gmailUsername.setEnabled(true);
        gmailPassword.setEnabled(true);
        // turn on server
        serverIsOn = false;
        exec.shutdownNow();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jTabbedPane = new javax.swing.JTabbedPane();
        envelopesTab = new javax.swing.JPanel();
        summaryScrollPane = new javax.swing.JScrollPane();
        summaryPanel = new javax.swing.JPanel();
        AccountsScrollPane = new javax.swing.JScrollPane();
        accountsTable = new javax.swing.JTable();
        newAccountField = new javax.swing.JTextField();
        addAccountButton = new javax.swing.JButton();
        EnvelopesScrollPane = new javax.swing.JScrollPane();
        envelopesTable = new javax.swing.JTable();
        categorizedCheckBox = new javax.swing.JCheckBox();
        newEnvelopeField = new javax.swing.JTextField();
        addEnvelopeButton = new javax.swing.JButton();
        newCategoryField = new javax.swing.JTextField();
        addCategoryButton = new javax.swing.JButton();
        transactionsScrollPane = new javax.swing.JScrollPane();
        transactionsPanel = new javax.swing.JPanel();
        transactionsTableScrollPane = new javax.swing.JScrollPane();
        transactionsTable = new javax.swing.JTable();
        transactionsLabel = new javax.swing.JLabel();
        dateRangeCheckBox = new javax.swing.JCheckBox();
        fromLabel = new javax.swing.JLabel();
        transFromField = new javax.swing.JTextField();
        toLabel = new javax.swing.JLabel();
        transToField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        transAccountDropdown = new javax.swing.JComboBox();
        transCategoryDropdown = new javax.swing.JComboBox();
        transEnvelopeDropdown = new javax.swing.JComboBox();
        exportTransactionsButton = new javax.swing.JButton();
        transactionDateField = new javax.swing.JTextField();
        transactionDescriptionField = new javax.swing.JTextField();
        transactionAmtField = new javax.swing.JTextField();
        dateLabel = new javax.swing.JLabel();
        amountLabel = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        newTransAcctDropdown = new javax.swing.JComboBox();
        newTransEnvDropdown = new javax.swing.JComboBox();
        newTransactionButton = new javax.swing.JButton();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        acctTransferFrom = new javax.swing.JComboBox();
        acctTransferTo = new javax.swing.JComboBox();
        envTransferFrom = new javax.swing.JComboBox();
        envTransferTo = new javax.swing.JComboBox();
        acctTransferButton = new javax.swing.JButton();
        envTransferButton = new javax.swing.JButton();
        selectedAcctAmt = new javax.swing.JLabel();
        selectedEnvAmt = new javax.swing.JLabel();
        transactionsRefreshButton = new javax.swing.JButton();
        hideTransfersToggleButton = new javax.swing.JCheckBox();
        removeEnvelopeButton = new javax.swing.JButton();
        removeAccountButton = new javax.swing.JButton();
        removeCategoryButton = new javax.swing.JButton();
        setCategoryButton = new javax.swing.JButton();
        mergeEnvelopesList = new javax.swing.JComboBox();
        mergeEnvelopesButton = new javax.swing.JButton();
        transactionQtyTextField = new javax.swing.JTextField();
        selectedAcctAmtLabel = new javax.swing.JLabel();
        selectedEnvAmtLabel = new javax.swing.JLabel();
        message = new javax.swing.JLabel();
        adminTab = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        emailPane = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        emailTable = new javax.swing.JTable();
        emailAddressDropdown = new javax.swing.JComboBox();
        blockEmail = new javax.swing.JButton();
        emailUserDropdown = new javax.swing.JComboBox();
        allowEmail = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        accountManagementPane = new javax.swing.JPanel();
        addUserTextField = new javax.swing.JTextField();
        addUserPasswordField = new javax.swing.JPasswordField();
        addUserButton = new javax.swing.JButton();
        removeUserDropdown = new javax.swing.JComboBox();
        removeUserButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        updateUserDropdown = new javax.swing.JComboBox();
        updateUserTextField = new javax.swing.JTextField();
        updateUserPasswordField = new javax.swing.JPasswordField();
        updateUserButton = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        gmailUsername = new javax.swing.JTextField();
        gmailPassword = new javax.swing.JPasswordField();
        jLabel8 = new javax.swing.JLabel();
        gmailServerStatus = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        serverToggleButton = new javax.swing.JToggleButton();
        usersMessage = new javax.swing.JLabel();
        reportsPanel = new javax.swing.JPanel();
        jLabel36 = new javax.swing.JLabel();
        trendReportDirectionsLabel = new javax.swing.JLabel();
        intervalTypeDropdown = new javax.swing.JComboBox();
        jLabel37 = new javax.swing.JLabel();
        intervalCountTextField = new javax.swing.JTextField();
        runTrendReportButton = new javax.swing.JButton();
        allTransactionsButton = new javax.swing.JButton();
        snapshotButton = new javax.swing.JButton();
        trendTextField = new javax.swing.JTextField();
        intervalTagLabel = new javax.swing.JLabel();
        reportProgressBar = new javax.swing.JProgressBar();
        budgetWorksheetButton = new javax.swing.JButton();
        jLabel30 = new javax.swing.JLabel();
        loginUserDropdown = new javax.swing.JComboBox();
        jLabel31 = new javax.swing.JLabel();
        userPassword = new javax.swing.JPasswordField();
        loginToggleButton = new javax.swing.JToggleButton();
        loginStatus = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        resetDatabaseMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        summaryPanel.setMaximumSize(new java.awt.Dimension(200, 5000));

        accountsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        accountsTable.setAutoscrolls(false);
        accountsTable.setFillsViewportHeight(true);
        accountsTable.setFocusable(false);
        accountsTable.setName(""); // NOI18N
        accountsTable.setRowSelectionAllowed(false);
        accountsTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                accountsTablePropertyChange(evt);
            }
        });
        AccountsScrollPane.setViewportView(accountsTable);

        newAccountField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                newAccountFieldKeyPressed(evt);
            }
        });

        addAccountButton.setText("Add Acct");
        addAccountButton.setEnabled(false);
        addAccountButton.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                addAccountButtonFocusLost(evt);
            }
        });
        addAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAccountButtonActionPerformed(evt);
            }
        });

        envelopesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        envelopesTable.setFillsViewportHeight(true);
        envelopesTable.setFocusable(false);
        envelopesTable.setRowSelectionAllowed(false);
        envelopesTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                envelopesTablePropertyChange(evt);
            }
        });
        EnvelopesScrollPane.setViewportView(envelopesTable);

        categorizedCheckBox.setSelected(true);
        categorizedCheckBox.setText("Categorized");
        categorizedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                categorizedCheckBoxActionPerformed(evt);
            }
        });

        newEnvelopeField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                newEnvelopeFieldKeyPressed(evt);
            }
        });

        addEnvelopeButton.setText("Add Env");
        addEnvelopeButton.setEnabled(false);
        addEnvelopeButton.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                addEnvelopeButtonFocusLost(evt);
            }
        });
        addEnvelopeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addEnvelopeButtonActionPerformed(evt);
            }
        });

        newCategoryField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                newCategoryFieldKeyPressed(evt);
            }
        });

        addCategoryButton.setText("Add Cat");
        addCategoryButton.setEnabled(false);
        addCategoryButton.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                addCategoryButtonFocusLost(evt);
            }
        });
        addCategoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCategoryButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout summaryPanelLayout = new javax.swing.GroupLayout(summaryPanel);
        summaryPanel.setLayout(summaryPanelLayout);
        summaryPanelLayout.setHorizontalGroup(
            summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(summaryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, summaryPanelLayout.createSequentialGroup()
                            .addComponent(newEnvelopeField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(addEnvelopeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, summaryPanelLayout.createSequentialGroup()
                            .addComponent(newAccountField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(addAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(EnvelopesScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addComponent(AccountsScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addGroup(summaryPanelLayout.createSequentialGroup()
                        .addComponent(newCategoryField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addCategoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(categorizedCheckBox))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        summaryPanelLayout.setVerticalGroup(
            summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, summaryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newAccountField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addAccountButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AccountsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addEnvelopeButton)
                    .addComponent(newEnvelopeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(EnvelopesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(categorizedCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addCategoryButton)
                    .addComponent(newCategoryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(17, 17, 17))
        );

        summaryScrollPane.setViewportView(summaryPanel);

        transactionsScrollPane.setPreferredSize(new java.awt.Dimension(926, 716));

        transactionsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        transactionsTable.setFillsViewportHeight(true);
        transactionsTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                transactionsTablePropertyChange(evt);
            }
        });
        transactionsTableScrollPane.setViewportView(transactionsTable);

        transactionsLabel.setText("Transactions");

        dateRangeCheckBox.setText("By Date Range");
        dateRangeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dateRangeCheckBoxActionPerformed(evt);
            }
        });

        fromLabel.setText("From:");

        transFromField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        transFromField.setEnabled(false);

        toLabel.setText("To:");

        transToField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        transToField.setEnabled(false);

        jLabel1.setText("Account:");

        jLabel27.setText("Set Envelope Category to:");

        jLabel3.setText("Merge Envelope into:");

        jLabel2.setText("Envelope:");

        transAccountDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        transAccountDropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transAccountDropdownActionPerformed(evt);
            }
        });

        transCategoryDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        transEnvelopeDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        transEnvelopeDropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transEnvelopeDropdownActionPerformed(evt);
            }
        });

        exportTransactionsButton.setText("Export Transactions");
        exportTransactionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTransactionsButtonActionPerformed(evt);
            }
        });

        transactionDateField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        transactionDateField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                transactionDateFieldFocusLost(evt);
            }
        });

        transactionDescriptionField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                transactionDescriptionFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                transactionDescriptionFieldFocusLost(evt);
            }
        });

        transactionAmtField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        transactionAmtField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                transactionAmtFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                transactionAmtFieldFocusLost(evt);
            }
        });

        dateLabel.setText("Date:");

        amountLabel.setText("Amt:");

        jLabel25.setText("Desc:");

        newTransAcctDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        newTransEnvDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        newTransactionButton.setText("Add Trans");
        newTransactionButton.setEnabled(false);
        newTransactionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newTransactionButtonActionPerformed(evt);
            }
        });

        jLabel18.setText("Account/To:");

        jLabel19.setText("Envelope/From:");

        acctTransferFrom.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        acctTransferTo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        envTransferFrom.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        envTransferTo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        acctTransferButton.setText("Transfer");
        acctTransferButton.setEnabled(false);
        acctTransferButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acctTransferButtonActionPerformed(evt);
            }
        });

        envTransferButton.setText("Transfer");
        envTransferButton.setEnabled(false);
        envTransferButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                envTransferButtonActionPerformed(evt);
            }
        });

        selectedAcctAmt.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N

        selectedEnvAmt.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N

        transactionsRefreshButton.setText("Refresh");
        transactionsRefreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transactionsRefreshButtonActionPerformed(evt);
            }
        });

        hideTransfersToggleButton.setText("Hide Tx");
        hideTransfersToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideTransfersToggleButtonActionPerformed(evt);
            }
        });

        removeEnvelopeButton.setText("x");
        removeEnvelopeButton.setEnabled(false);
        removeEnvelopeButton.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                removeEnvelopeButtonFocusLost(evt);
            }
        });
        removeEnvelopeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeEnvelopeButtonActionPerformed(evt);
            }
        });

        removeAccountButton.setText("x");
        removeAccountButton.setEnabled(false);
        removeAccountButton.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                removeAccountButtonFocusLost(evt);
            }
        });
        removeAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAccountButtonActionPerformed(evt);
            }
        });

        removeCategoryButton.setText("x");
        removeCategoryButton.setEnabled(false);
        removeCategoryButton.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                removeCategoryButtonFocusLost(evt);
            }
        });
        removeCategoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeCategoryButtonActionPerformed(evt);
            }
        });

        setCategoryButton.setText("Set");
        setCategoryButton.setEnabled(false);
        setCategoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setCategoryButtonActionPerformed(evt);
            }
        });

        mergeEnvelopesList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        mergeEnvelopesButton.setText("Merge");
        mergeEnvelopesButton.setEnabled(false);
        mergeEnvelopesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mergeEnvelopesButtonActionPerformed(evt);
            }
        });

        transactionQtyTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        transactionQtyTextField.setText("250");
        transactionQtyTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                transactionQtyTextFieldKeyPressed(evt);
            }
        });

        selectedAcctAmtLabel.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        selectedAcctAmtLabel.setText("<acct amt>");

        selectedEnvAmtLabel.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        selectedEnvAmtLabel.setText("<env amt>");

        javax.swing.GroupLayout transactionsPanelLayout = new javax.swing.GroupLayout(transactionsPanel);
        transactionsPanel.setLayout(transactionsPanelLayout);
        transactionsPanelLayout.setHorizontalGroup(
            transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transactionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(transactionsTableScrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(transAccountDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(removeAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(24, 24, 24)
                                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel2)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(removeEnvelopeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(transEnvelopeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(18, 18, 18)
                                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabel3)
                                            .addComponent(jLabel27))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                                .addComponent(transCategoryDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(setCategoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(removeCategoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                                .addComponent(mergeEnvelopesList, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(mergeEnvelopesButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                                        .addComponent(selectedEnvAmtLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(selectedEnvAmt))))
                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                .addComponent(transactionQtyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transactionsLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(dateRangeCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(hideTransfersToggleButton)
                                .addGap(18, 18, 18)
                                .addComponent(fromLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transFromField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(toLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transToField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(transactionsRefreshButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(exportTransactionsButton))
                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(selectedAcctAmtLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(selectedAcctAmt))
                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(transactionDateField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(dateLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(transactionDescriptionField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel25))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(transactionAmtField, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(amountLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                                .addComponent(envTransferFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(envTransferTo, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                                .addComponent(acctTransferFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(acctTransferTo, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(acctTransferButton)
                                            .addComponent(envTransferButton)))
                                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(newTransAcctDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel19))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel18)
                                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                                .addComponent(newTransEnvDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(newTransactionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)))))))
                        .addGap(0, 56, Short.MAX_VALUE)))
                .addContainerGap())
        );
        transactionsPanelLayout.setVerticalGroup(
            transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transactionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(dateRangeCheckBox)
                            .addComponent(transFromField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fromLabel)
                            .addComponent(transToField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(toLabel)
                            .addComponent(transactionsRefreshButton)
                            .addComponent(hideTransfersToggleButton)
                            .addComponent(exportTransactionsButton)
                            .addComponent(transactionsLabel)
                            .addComponent(transactionQtyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(19, 19, 19)
                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transAccountDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(transactionsPanelLayout.createSequentialGroup()
                                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(removeEnvelopeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(removeAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transEnvelopeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(mergeEnvelopesList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mergeEnvelopesButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(transCategoryDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel27)
                                .addComponent(setCategoryButton))
                            .addComponent(removeCategoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(transactionsPanelLayout.createSequentialGroup()
                        .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(selectedAcctAmt)
                            .addComponent(selectedEnvAmt))
                        .addGap(28, 28, 28))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(selectedAcctAmtLabel)
                        .addComponent(selectedEnvAmtLabel)))
                .addGap(17, 17, 17)
                .addComponent(transactionsTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 355, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel19)
                        .addComponent(jLabel18)
                        .addComponent(amountLabel)
                        .addComponent(jLabel25))
                    .addComponent(dateLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newTransAcctDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(newTransEnvDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(newTransactionButton)
                    .addComponent(transactionAmtField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transactionDescriptionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transactionDateField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(acctTransferFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(acctTransferTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(acctTransferButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(transactionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(envTransferFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(envTransferTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(envTransferButton))
                .addContainerGap())
        );

        transactionsScrollPane.setViewportView(transactionsPanel);

        message.setForeground(new java.awt.Color(255, 0, 0));
        message.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        javax.swing.GroupLayout envelopesTabLayout = new javax.swing.GroupLayout(envelopesTab);
        envelopesTab.setLayout(envelopesTabLayout);
        envelopesTabLayout.setHorizontalGroup(
            envelopesTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(envelopesTabLayout.createSequentialGroup()
                .addComponent(summaryScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(envelopesTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(envelopesTabLayout.createSequentialGroup()
                        .addComponent(message)
                        .addContainerGap(796, Short.MAX_VALUE))
                    .addComponent(transactionsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );
        envelopesTabLayout.setVerticalGroup(
            envelopesTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(summaryScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(envelopesTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(message)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(transactionsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
        );

        jTabbedPane.addTab("Envelopes", envelopesTab);

        emailPane.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "EMAIL LOGINS", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 18))); // NOI18N

        emailTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        emailTable.setFillsViewportHeight(true);
        jScrollPane2.setViewportView(emailTable);

        emailAddressDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        blockEmail.setText("Block");
        blockEmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blockEmailActionPerformed(evt);
            }
        });

        emailUserDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        allowEmail.setText("Assign");
        allowEmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allowEmailActionPerformed(evt);
            }
        });

        jLabel11.setText("Addr:");

        jLabel16.setText("User:");

        javax.swing.GroupLayout emailPaneLayout = new javax.swing.GroupLayout(emailPane);
        emailPane.setLayout(emailPaneLayout);
        emailPaneLayout.setHorizontalGroup(
            emailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(emailPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(emailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(emailPaneLayout.createSequentialGroup()
                        .addGroup(emailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel16)
                            .addComponent(jLabel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(emailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(emailPaneLayout.createSequentialGroup()
                                .addComponent(emailUserDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(allowEmail))
                            .addGroup(emailPaneLayout.createSequentialGroup()
                                .addComponent(emailAddressDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(blockEmail)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 613, Short.MAX_VALUE))
                .addContainerGap())
        );
        emailPaneLayout.setVerticalGroup(
            emailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(emailPaneLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(emailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(emailAddressDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(blockEmail)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(emailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(emailUserDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(allowEmail)
                    .addComponent(jLabel16))
                .addGap(10, 10, 10))
        );

        accountManagementPane.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "ACCOUNT MGMT", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 18))); // NOI18N

        addUserButton.setText("Add");
        addUserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addUserButtonActionPerformed(evt);
            }
        });
        addUserButton.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                addUserButtonFocusLost(evt);
            }
        });

        removeUserDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        removeUserButton.setText("Remove");
        removeUserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeUserButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Username:");

        jLabel6.setText("Password:");

        jLabel14.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel14.setText("NEW USERS");

        jLabel15.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel15.setText("UPDATE USERS");

        updateUserDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        updateUserButton.setText("Update");
        updateUserButton.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                updateUserButtonFocusLost(evt);
            }
        });
        updateUserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateUserButtonActionPerformed(evt);
            }
        });

        jLabel21.setText("New pw:");

        jLabel22.setText("Username:");

        jLabel26.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel26.setText("REMOVE USERS");

        jLabel29.setText("Username:");

        jLabel10.setText("New u/n:");

        javax.swing.GroupLayout accountManagementPaneLayout = new javax.swing.GroupLayout(accountManagementPane);
        accountManagementPane.setLayout(accountManagementPaneLayout);
        accountManagementPaneLayout.setHorizontalGroup(
            accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accountManagementPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(accountManagementPaneLayout.createSequentialGroup()
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel22, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel21, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(updateUserDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(accountManagementPaneLayout.createSequentialGroup()
                                .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(updateUserTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(updateUserPasswordField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(updateUserButton, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(accountManagementPaneLayout.createSequentialGroup()
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(addUserTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(addUserPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addUserButton, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel14)
                    .addComponent(jLabel15)
                    .addGroup(accountManagementPaneLayout.createSequentialGroup()
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(accountManagementPaneLayout.createSequentialGroup()
                                .addComponent(jLabel29)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removeUserDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel26))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeUserButton, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        accountManagementPaneLayout.setVerticalGroup(
            accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, accountManagementPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(accountManagementPaneLayout.createSequentialGroup()
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(addUserTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(addUserPasswordField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6)))
                    .addComponent(addUserButton, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(updateUserDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel22))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(accountManagementPaneLayout.createSequentialGroup()
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(updateUserTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(updateUserPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel21))
                        .addGap(19, 19, 19))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, accountManagementPaneLayout.createSequentialGroup()
                        .addComponent(updateUserButton, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24)))
                .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(accountManagementPaneLayout.createSequentialGroup()
                        .addComponent(jLabel26)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(accountManagementPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel29)
                            .addComponent(removeUserDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(removeUserButton, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(44, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "GMAIL SERVER", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 18))); // NOI18N

        jLabel9.setText("Create a Gmail account specifically for this program and enter it below.");

        jLabel7.setText("Username:");

        jLabel8.setText("Password:");

        gmailServerStatus.setText("Server is currently off.");

        jLabel20.setText("@gmail.com");

        jLabel23.setText("(Once server is running, send commands to that address via text or email)");

        serverToggleButton.setText("Start Server");
        serverToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverToggleButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 432, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel8)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(gmailUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel20))
                            .addComponent(gmailPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(6, 6, 6)
                        .addComponent(serverToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(gmailServerStatus)
                    .addComponent(jLabel23))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel23)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(gmailUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel20))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(gmailPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8)))
                    .addComponent(serverToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gmailServerStatus)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        usersMessage.setForeground(new java.awt.Color(255, 0, 0));

        reportsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "REPORTS", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 18))); // NOI18N

        jLabel36.setText("Interval:");

        trendReportDirectionsLabel.setText("For trend report, enter desired accounts and/or envelopes (comma separated):");

        intervalTypeDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        intervalTypeDropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intervalTypeDropdownActionPerformed(evt);
            }
        });

        jLabel37.setText("for");

        intervalCountTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        intervalCountTextField.setText("12");
        intervalCountTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                intervalCountTextFieldFocusLost(evt);
            }
        });

        runTrendReportButton.setText("Run Trend Report");
        runTrendReportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runTrendReportButtonActionPerformed(evt);
            }
        });

        allTransactionsButton.setText("Export Transactions");
        allTransactionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allTransactionsButtonActionPerformed(evt);
            }
        });

        snapshotButton.setText("Get 12-Mth Snapshot");
        snapshotButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snapshotButtonActionPerformed(evt);
            }
        });

        trendTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                trendTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                trendTextFieldFocusLost(evt);
            }
        });

        intervalTagLabel.setText("months");

        budgetWorksheetButton.setText("Get Budget W/S");
        budgetWorksheetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                budgetWorksheetButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout reportsPanelLayout = new javax.swing.GroupLayout(reportsPanel);
        reportsPanel.setLayout(reportsPanelLayout);
        reportsPanelLayout.setHorizontalGroup(
            reportsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(reportsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(reportsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(trendReportDirectionsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(reportsPanelLayout.createSequentialGroup()
                        .addGroup(reportsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(reportsPanelLayout.createSequentialGroup()
                                .addComponent(jLabel36)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(intervalTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel37)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(intervalCountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(intervalTagLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(runTrendReportButton, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(reportProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(reportsPanelLayout.createSequentialGroup()
                                .addComponent(snapshotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(budgetWorksheetButton, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(allTransactionsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(trendTextField))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        reportsPanelLayout.setVerticalGroup(
            reportsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(reportsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(reportProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 9, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(reportsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(snapshotButton)
                    .addComponent(allTransactionsButton)
                    .addComponent(budgetWorksheetButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(trendReportDirectionsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(trendTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(reportsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel36)
                    .addComponent(intervalTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel37)
                    .addComponent(intervalCountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(intervalTagLabel)
                    .addComponent(runTrendReportButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(usersMessage)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(accountManagementPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(reportsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(emailPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(usersMessage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(emailPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(reportsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(accountManagementPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(152, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel2);

        javax.swing.GroupLayout adminTabLayout = new javax.swing.GroupLayout(adminTab);
        adminTab.setLayout(adminTabLayout);
        adminTabLayout.setHorizontalGroup(
            adminTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(adminTabLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1024, Short.MAX_VALUE)
                .addContainerGap())
        );
        adminTabLayout.setVerticalGroup(
            adminTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(adminTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 665, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Admin", adminTab);

        jLabel30.setText("U/N:");

        loginUserDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel31.setText("PW:");

        userPassword.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                userPasswordKeyPressed(evt);
            }
        });

        loginToggleButton.setText("Sign In");
        loginToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginToggleButtonActionPerformed(evt);
            }
        });

        loginStatus.setText("No one is currently logged in.");

        fileMenu.setText("File");

        aboutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(aboutMenuItem);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);
        fileMenu.add(jSeparator3);

        resetDatabaseMenuItem.setText("Reset DB");
        resetDatabaseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetDatabaseMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(resetDatabaseMenuItem);

        menuBar.add(fileMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel30)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loginUserDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel31)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(userPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loginToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(loginStatus)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel30)
                    .addComponent(loginUserDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(userPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel31)
                    .addComponent(loginToggleButton)
                    .addComponent(loginStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void userPasswordKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_userPasswordKeyPressed
        if (evt.getKeyCode() == 10) {
            attemptLogin();
        }
    }//GEN-LAST:event_userPasswordKeyPressed

    private void loginToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginToggleButtonActionPerformed
        if (loginToggleButton.isSelected()) { // attempt to log user in
            attemptLogin();
        } else { // log user out
            logout();
        }
    }//GEN-LAST:event_loginToggleButtonActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        JOptionPane.showMessageDialog(this,
                "ENVELOPES\n"
                + "\n"
                + "Version: 3.0\n"
                + "Released on: 2018-02-24\n"
                + "Updated on: " + VER + "\n"
                + "\n"
                + "This application allows multiple users to share funds and keep\n"
                + "track of spending in real-time and on the go. Simply setup a\n"
                + "dedicated Gmail account, log into that account from the \"Admin\"\n"
                + "tab and you're ready to go. Create Accounts where money actually\n"
                + "resides, and Envelopes where you want your money to go. As\n"
                + "money comes and goes, text or email commands to the Gmail\n"
                + "address you specified and this application will respond with\n"
                + "updates.\n"
                + "\n"
                + "NOTE: If this is your first time logging in, Admin password\n"
                + "is 'password'. For command usage format, text/email 'help' to\n"
                + "your specified Gmail account.");
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        String msg = "Are you sure you want to quit?";
        String title = "Closing " + TITLE;
        int yes = 0;
        int opt = JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt == yes) {
            System.exit(0);
        }
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void resetDatabaseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetDatabaseMenuItemActionPerformed
        String msg = "WARNING: database resets CANNOT be undone.\n"
                + "Are you sure you want to purge all data?";
        String title = "Reset Database";
        int yes = 0;
        int opt = JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == yes) {
            // log user (admin) out
            logout();
            // shutdown Gmail Server
            shutdownServer();
            // reset database
            mc.resetDatabase();
            // set sign in settings
            gmailUsername.setText("");
            gmailPassword.setText("");
            userPassword.setText("");
            // update all views
            updateAll();
        }
    }//GEN-LAST:event_resetDatabaseMenuItemActionPerformed

    private void budgetWorksheetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_budgetWorksheetButtonActionPerformed
        Runnable budgetWorksheet = new Runnable() {
            @Override
            public void run() {
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File("budget_" + Utilities.getDatestamp(0)));
                int returnVal = fc.showSaveDialog(thisConsole);
                String fileName;
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    fileName = file.getAbsolutePath();

                    // remove file extension so we can add it after incremental append to duplicates
                    if (fileName.endsWith(".csv")) {
                        fileName = fileName.substring(0, fileName.length() - 4);
                    }
                    File f = new File(fileName + ".csv");
                    int count = 1;
                    while (f.exists()) {
                        f = new File(fileName + "(" + count++ + ")" + ".csv");
                    }

                    // disable report buttons so only one report is run at once
                    snapshotButton.setEnabled(false);
                    allTransactionsButton.setEnabled(false);
                    runTrendReportButton.setEnabled(false);
                    budgetWorksheetButton.setEnabled(false);

                    // intialize variables
                    int row = 5;
                    String C = "=", D = "=", E = "=";

                    // setup for progress bar value
                    reportProgressBar.setValue(0);
                    int envCount = mc.getEnvelopeCCount() - 1;
                    int max = 5 + envCount;
                    int curr = 3;

                    // write budget worksheet to file
                    try (FileWriter writer = new FileWriter(f)) {
                        // write header info to file
                        writer.write("Budget prepared on " + Utilities.getDatestamp(0) + "\n\n");
                        writer.write(",Left Over,=-C" + (envCount + 6) + "\n\n");
                        writer.write("Envelope,,Budget Amt,Current Amt,New Amt\n");
                        reportProgressBar.setValue(curr * 100 / max);
                        // write categorized envelopes to file
                        for (int i = 0; i < envCount; i++) {
                            if (mc.isCategory(i)) {
                                String catName = mc.getEnvelopeCName(i);    // get category name
                                String[][] envs = mc.getEnvelopes(catName); // get envelopes in specified category
                                // update progress bar
                                curr++;
                                reportProgressBar.setValue(curr * 100 / max);
                                // update current row in spreadsheet
                                row++;
                                int a = row + 1;
                                int b = row + envs[0].length;
                                C += "C" + row + "+";
                                D += "D" + row + "+";
                                E += "E" + row + "+";
                                // print category
                                writer.write(catName.toUpperCase() + ",,=SUM(C" + a + ":C" + b + "),=SUM(D" + a + ":D" + b + "),=SUM(E" + a + ":E" + b + ")\n");
                                // print corresponding envelopes
                                for (int j = 0; j < envs[0].length; j++) {
                                    // update progress bar
                                    curr++;
                                    reportProgressBar.setValue(curr * 100 / max);
                                    // update current row in spreadsheet
                                    row++;
                                    writer.write("," + envs[0][j] + ",," + envs[1][j] + ",=C" + row + "+D" + row + "\n");
                                }
                            }
                        }

                        // write totals to file
                        if (C.length() > 0 && C.charAt(C.length() - 1) == '+') { // removes last '+' character
                            C = C.substring(0, C.length() - 1);
                            D = D.substring(0, D.length() - 1);
                            E = E.substring(0, E.length() - 1);
                            writer.write("TOTAL,," + C + "," + D + "," + E + "\n");
                        } else {
                            writer.write("TOTAL,," + 0 + "," + 0 + "," + 0 + "\n");
                        }
                        curr++;
                        reportProgressBar.setValue(curr * 100 / max);

                        writer.flush();
                    } catch (IOException ex) {
                        /* DO NOTHING */ }

                    reportProgressBar.setValue(0);
                    snapshotButton.setEnabled(true);
                    allTransactionsButton.setEnabled(true);
                    runTrendReportButton.setEnabled(true);
                    budgetWorksheetButton.setEnabled(true);
                }
            }

        };
        new Thread(budgetWorksheet).start();
    }//GEN-LAST:event_budgetWorksheetButtonActionPerformed

    private void trendTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_trendTextFieldFocusLost
        String input = trendTextField.getText();
        if (input.length() > 0) {
            input = input.toLowerCase();
            // remove invalid charaters
            String tmp = "";
            for (int i = 0; i < input.length(); i++) {
                if ((input.charAt(i) >= 'a' && input.charAt(i) <= 'z') || (input.charAt(i) == '-' && i > 0) || input.charAt(i) == ',') {
                    tmp += input.charAt(i);
                }
            }
            String[] names = tmp.split(",");
            tmp = "";
            LinkedList<String> validatedNames = new LinkedList();
            for (String s : names) {
                if (!validatedNames.contains(s)) { // removes duplicate entries
                    validatedNames.add(s);
                    if (mc.isAccount(s) || mc.isEnvelope(s)) {
                        tmp += s + ",";
                    } else {
                        tmp += "[" + s + "],";
                    }
                }
            }
            // removes last comma

            input = tmp.substring(0, tmp.length() - 1);
        } else {
            input = "";
        }
        trendTextField.setText(input);
        if (trendTextField.getText().contains("[")) {
            trendReportDirectionsLabel.setText("Names within [brackets] are invalid, please fix before continuing:");
            trendReportDirectionsLabel.setForeground(Color.red);
        }
    }//GEN-LAST:event_trendTextFieldFocusLost

    private void trendTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_trendTextFieldFocusGained
        trendReportDirectionsLabel.setText("For trend report, enter desired accounts and/or envelopes (comma separated):");
        trendReportDirectionsLabel.setForeground(Color.BLACK);
    }//GEN-LAST:event_trendTextFieldFocusGained

    private void snapshotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_snapshotButtonActionPerformed
        Runnable snapshotReport = new Runnable() {
            @Override
            public void run() {
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File("snapshot_" + Utilities.getDatestamp(0)));
                int returnVal = fc.showSaveDialog(thisConsole);
                String fileName;
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    fileName = file.getAbsolutePath();

                    // remove file extension so we can add it after incremental append to duplicates
                    if (fileName.endsWith(".csv")) {
                        fileName = fileName.substring(0, fileName.length() - 4);
                    }
                    File f = new File(fileName + ".csv");
                    int count = 1;
                    while (f.exists()) {
                        f = new File(fileName + "(" + count++ + ")" + ".csv");
                    }
                    // disable report buttons so only one report is run at once
                    snapshotButton.setEnabled(false);
                    allTransactionsButton.setEnabled(false);
                    runTrendReportButton.setEnabled(false);
                    budgetWorksheetButton.setEnabled(false);
                    // setup for progress bar value
                    reportProgressBar.setValue(0);
                    int acctCount = mc.getAccountCount() - 1;
                    int envCount = mc.getEnvelopeUCount() - 1;
                    int max = 50 + acctCount + acctCount * 13 + envCount + envCount * 13;
                    int curr = acctCount + envCount;
                    reportProgressBar.setValue(curr * 100 / max);
                    // Get 12 most recent months:
                    String[][] twelveMths = new String[12][2];
                    // finds next month based off today's date
                    int mth = ((Integer.parseInt(Utilities.getDatestamp(0).substring(5, 7))) % 12) + 1;
                    // finds the year of next month
                    int yr = Integer.parseInt(Utilities.getDatestamp(0).substring(0, 4));
                    if (mth == 1) {
                        yr++;
                    }
                    String date;
                    for (int i = 11; i >= 0; i--) {
                        if (mth < 10) { // one digit
                            date = Integer.toString(yr) + "-0" + Integer.toString(mth) + "-01";
                            if (mth == 1) { // reset month to Dec and decrease year
                                mth = 12;
                                yr--;
                            } else {
                                mth--;  // decrease month, same year
                            }
                        } else {
                            date = Integer.toString(yr) + "-" + Integer.toString(mth) + "-01"; // two digit month (10-11)
                            mth--;      // decrease month, same year
                        }
                        twelveMths[i][0] = date;
                        switch (Integer.parseInt(date.substring(5, 7))) {
                            case 2:
                                twelveMths[i][1] = "(Jan";
                                break;
                            case 3:
                                twelveMths[i][1] = "(Feb";
                                break;
                            case 4:
                                twelveMths[i][1] = "(Mar";
                                break;
                            case 5:
                                twelveMths[i][1] = "(Apr";
                                break;
                            case 6:
                                twelveMths[i][1] = "(May";
                                break;
                            case 7:
                                twelveMths[i][1] = "(Jun";
                                break;
                            case 8:
                                twelveMths[i][1] = "(Jul";
                                break;
                            case 9:
                                twelveMths[i][1] = "(Aug";
                                break;
                            case 10:
                                twelveMths[i][1] = "(Sep";
                                break;
                            case 11:
                                twelveMths[i][1] = "(Oct";
                                break;
                            case 12:
                                twelveMths[i][1] = "(Nov";
                                break;
                            default:
                                twelveMths[i][1] = "(Dec";
                                break;
                        }
                        twelveMths[i][1] += " " + date.substring(2, 4) + ")";
                        curr++;
                        reportProgressBar.setValue(curr * 100 / max);
                    }

                    // write snapshot to file
                    try (FileWriter writer = new FileWriter(f)) {
                        writer.write("Snapshot as of " + Utilities.getDatestamp(0));

                        // WRITE ACCOUNTS TO FILE
                        writer.write("\n\nAccounts");
                        // write date header for each month, 'Mmm YY' format
                        for (int i = 0; i < 12; i++) {
                            writer.write("," + twelveMths[i][1]);
                            curr++;
                            reportProgressBar.setValue(curr * 100 / max);
                        }
                        for (int i = 0; i < acctCount; i++) {
                            // write account name
                            writer.write("\n" + mc.getAccountName(i));
                            curr++;
                            reportProgressBar.setValue(curr * 100 / max);
                            // write account totals by month
                            for (int j = 0; j < 12; j++) {
                                writer.write("," + Utilities.amountToStringSimple(mc.getAccountAmount(mc.getAccountName(i), twelveMths[j][0])));
                                curr++;
                                reportProgressBar.setValue(curr * 100 / max);
                            }
                        }
                        // totals for each month
                        writer.write("\nTotal");
                        for (int i = 0; i < 12; i++) {
                            writer.write("," + Utilities.amountToStringSimple(mc.getAccountAmount("ALL", twelveMths[i][0])));
                            curr++;
                            reportProgressBar.setValue(curr * 100 / max);
                        }

                        // WRITE ENVELOPES TO FILE
                        writer.write("\n\nEnvelopes");
                        // write date header for each month, 'Mmm YY' format
                        for (int i = 0; i < 12; i++) {
                            writer.write("," + twelveMths[i][1]);
                            curr++;
                            reportProgressBar.setValue(curr * 100 / max);
                        }
                        for (int i = 0; i < envCount; i++) {
                            // write envelope name
                            writer.write("\n" + mc.getEnvelopeUName(i));
                            curr++;
                            reportProgressBar.setValue(curr * 100 / max);
                            // write envelope totals by month
                            for (int j = 0; j < 12; j++) {
                                writer.write("," + Utilities.amountToStringSimple(mc.getEnvelopeAmount(mc.getEnvelopeUName(i), twelveMths[j][0])));
                                curr++;
                                reportProgressBar.setValue(curr * 100 / max);
                            }
                        }
                        // totals for each month
                        writer.write("\nTotal");
                        for (int i = 0; i < 12; i++) {
                            writer.write("," + Utilities.amountToStringSimple(mc.getEnvelopeAmount("ALL", twelveMths[i][0])));
                            curr++;
                            reportProgressBar.setValue(curr * 100 / max);
                        }
                        writer.flush();
                    } catch (IOException ex) {
                        /* DO NOTHING */ }
                    reportProgressBar.setValue(0);
                    snapshotButton.setEnabled(true);
                    allTransactionsButton.setEnabled(true);
                    runTrendReportButton.setEnabled(true);
                    budgetWorksheetButton.setEnabled(true);
                }
            }

        };
        new Thread(snapshotReport).start();
    }//GEN-LAST:event_snapshotButtonActionPerformed

    private void allTransactionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allTransactionsButtonActionPerformed
        Runnable exportAllTransactions = new Runnable() {
            @Override
            public void run() {
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File("transactions_" + Utilities.getDatestamp(0)));
                int returnVal = fc.showSaveDialog(thisConsole);
                String fileName;
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    fileName = file.getAbsolutePath();
                    // remove file extension so we can add it after incremental append to duplicates
                    if (fileName.endsWith(".csv")) {
                        fileName = fileName.substring(0, fileName.length() - 4);
                    }
                    File f = new File(fileName + ".csv");
                    int count = 1;
                    while (f.exists()) {
                        f = new File(fileName + "(" + count++ + ")" + ".csv");
                    }
                    // disable report buttons so only one report is run at once
                    snapshotButton.setEnabled(false);
                    allTransactionsButton.setEnabled(false);
                    runTrendReportButton.setEnabled(false);
                    budgetWorksheetButton.setEnabled(false);

                    // get all transactions
                    String[] trans = mc.getAllTransactions(reportProgressBar);
                    int curr = 0;
                    int max = trans.length;

                    // write transactions to file
                    try (FileWriter writer = new FileWriter(f)) {
                        if (trans.length > 1) { // writes only if there are 1 or more transactions
                            writer.write("Transactions as of " + Utilities.getDatestamp(0) + "\n\n");
                            for (String t : trans) {
                                writer.write(t);
                                reportProgressBar.setValue((++curr) * 100 / max);
                            }
                        } else {
                            writer.write("Transactions as of " + Utilities.getDatestamp(0) + "\n\n");
                            writer.write("<No transactions...>");
                        }
                        writer.flush();
                    } catch (IOException ex) { /* DO NOTHING */ }
                    // enable reports buttons now that transaction pull is complete
                    reportProgressBar.setValue(0);
                    snapshotButton.setEnabled(true);
                    allTransactionsButton.setEnabled(true);
                    runTrendReportButton.setEnabled(true);
                    budgetWorksheetButton.setEnabled(true);
                }
            }
        };
        new Thread(exportAllTransactions).start();
    }//GEN-LAST:event_allTransactionsButtonActionPerformed

    private void runTrendReportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runTrendReportButtonActionPerformed
        Runnable runTrendReport = new Runnable() {
            @Override
            public void run() {
                String validatedUserInput = trendTextField.getText();
                if(validatedUserInput.length()==0) {
                    trendReportDirectionsLabel.setText("At least one account or envelope name must be specified:");
                    trendReportDirectionsLabel.setForeground(Color.red);
                } else if(!validatedUserInput.contains("[")) {
                    JFileChooser fc = new JFileChooser();
                    fc.setSelectedFile(new File("trend_" + Utilities.getDatestamp(0)));
                    int returnVal = fc.showSaveDialog(thisConsole);
                    String fileName;
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        fileName = file.getAbsolutePath();
                        // remove file extension so we can add it after incremental append to duplicates
                        if(fileName.endsWith(".csv")) {
                            fileName = fileName.substring(0, fileName.length()-4);
                        }
                        File f = new File(fileName + ".csv");
                        int count = 1;
                        while(f.exists()) {
                            f = new File(fileName + "(" + count++ + ")" + ".csv");
                        }
                        // get dates (first column of report)
                        String[] dates = Utilities.getDatesByInterval(intervalTypeDropdown.getSelectedIndex(), Integer.parseInt(intervalCountTextField.getText()));
                        // get account and envelope names (2nd through last column of report)
                        String[]acctAndEnvNames = validatedUserInput.split(",");
                        // disable report buttons so only one report is run at once
                        snapshotButton.setEnabled(false);
                        allTransactionsButton.setEnabled(false);
                        runTrendReportButton.setEnabled(false);
                        budgetWorksheetButton.setEnabled(false);
                        trendTextField.setText("");
                        // setup for progress bar value
                        reportProgressBar.setValue(0);
                        int max = dates.length;
                        int curr = 0;
                        // write trends to file
                        try (FileWriter writer = new FileWriter(f)) {
                            writer.write("Trends as of " + Utilities.getDatestamp(0) + "\n\n");
                            // write column headers (account and envelope names)
                            for(String name : acctAndEnvNames) {
                                if(mc.isAccount(name)) {
                                    writer.write(",*" + name);
                                } else {
                                    writer.write("," + name);
                                }
                            }
                            for (String date : dates) {
                                writer.write("\n" + date);
                                for (String name : acctAndEnvNames) {
                                    if(mc.isAccount(name)) {
                                        writer.write("," + Utilities.amountToStringSimple(mc.getAccountAmount(name, Utilities.getNewDate(date, 1))));
                                    } else {
                                        writer.write("," + Utilities.amountToStringSimple(mc.getEnvelopeAmount(name, Utilities.getNewDate(date, 1))));
                                    }
                                }
                                curr++;
                                reportProgressBar.setValue(curr*100/max);
                            }
                            writer.flush();
                        } catch (IOException ex) { /* DO NOTHING */ }
                        // enable reports buttons now that transaction pull is complete
                        reportProgressBar.setValue(0);
                        snapshotButton.setEnabled(true);
                        allTransactionsButton.setEnabled(true);
                        runTrendReportButton.setEnabled(true);
                        budgetWorksheetButton.setEnabled(true);
                    }
                }
            }
        };
        new Thread(runTrendReport).start();
    }//GEN-LAST:event_runTrendReportButtonActionPerformed

    private void intervalCountTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_intervalCountTextFieldFocusLost
        // removes non-digit characters
        String input = intervalCountTextField.getText();
        String tmp = "";
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                tmp += input.charAt(i);
            }
        }

        int dur;
        // checks for numbers too big for integer to hold
        try {
            dur = Integer.parseInt(tmp);
        } catch (NumberFormatException ex) {
            dur = 36500;
        }

        switch (intervalTypeDropdown.getSelectedIndex()) {
            case 0:
                // monthly
                // enforces 6 to 1200 months
                if (dur < 6) {
                    intervalCountTextField.setText("6");
                } else if (dur > 1200) {
                    intervalCountTextField.setText("1200");
                } else {
                    intervalCountTextField.setText(Integer.toString(dur));
                }
                break;
            case 1:
                // weekly (every 7 days)
                // enforces 4 to 5200 weeks
                if (dur < 4) {
                    intervalCountTextField.setText("4");
                } else if (dur > 5200) {
                    intervalCountTextField.setText("5200");
                } else {
                    intervalCountTextField.setText(Integer.toString(dur));
                }
                break;
            default:
                // daily
                // enforces 7 to 36500 days
                if (dur < 7) {
                    intervalCountTextField.setText("7");
                } else if (dur > 36500) {
                    intervalCountTextField.setText("36500");
                } else {
                    intervalCountTextField.setText(Integer.toString(dur));
                }
                break;
        }

    }//GEN-LAST:event_intervalCountTextFieldFocusLost

    private void intervalTypeDropdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intervalTypeDropdownActionPerformed
        switch (intervalTypeDropdown.getSelectedIndex()) {
            case 0:
                // monthly
                intervalTagLabel.setText("months");
                intervalCountTextField.setText("12");
                break;
            case 1:
                // weekly (every 7 days)
                intervalTagLabel.setText("weeks");
                intervalCountTextField.setText("52");
                break;
            default:
                // daily
                intervalTagLabel.setText("days");
                intervalCountTextField.setText("365");
                break;
        }
    }//GEN-LAST:event_intervalTypeDropdownActionPerformed

    private void serverToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverToggleButtonActionPerformed
        if (serverToggleButton.isSelected()) {
            attemptStartServer();
        } else {
            shutdownServer();
        }
    }//GEN-LAST:event_serverToggleButtonActionPerformed

    private void updateUserButtonFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_updateUserButtonFocusLost
        usersMessage.setText("");
    }//GEN-LAST:event_updateUserButtonFocusLost

    private void updateUserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateUserButtonActionPerformed
        String oldUsername = (String) updateUserDropdown.getSelectedItem();
        String newUsername = updateUserTextField.getText();
        String pw = "";
        char[] password = updateUserPasswordField.getPassword();
        for (char c : password) {
            pw += c;
        }
        newUsername = newUsername.trim();
        pw = pw.trim();
        if (newUsername.length() == 0 && pw.length() == 0) {
            // do nothing
        } else if (newUsername.length() == 0) {
            // update only password
            if (Utilities.isValidPassword(pw)) {
                mc.setUserPassword(oldUsername, pw);
                updateUserPasswordField.setText("");
            } else {
                usersMessage.setText("ERROR: password must contain at least 4 characters with no whitespaces.");
            }
        } else if (pw.length() == 0) {
            // update only username
            if (Utilities.isValidUsername(newUsername)) {
                if (oldUsername.equalsIgnoreCase(currUser)) {
                    currUser = newUsername;
                    loginStatus.setText("Welcome " + currUser + "! You are now signed in.");
                }
                mc.renameUser(oldUsername, newUsername);
                updateUserDropdowns();
                updateEmailTable();
                updateUserTextField.setText("");
            } else {
                usersMessage.setText("ERROR: username must begin with a letter and contain only letters and numbers.");
            }
        } else {
            // update both username and password
            if (!Utilities.isValidUsername(newUsername)) {
                usersMessage.setText("ERROR: username must begin with a letter and contain only letters and numbers.");
            } else if (!Utilities.isValidPassword(pw)) {
                usersMessage.setText("ERROR: password must contain at least 4 characters with no whitespaces.");
            } else {
                mc.setUserPassword(oldUsername, pw);
                if (oldUsername.equalsIgnoreCase(currUser)) {
                    currUser = newUsername;
                    loginStatus.setText("Welcome " + currUser + "! You are now signed in.");
                }
                mc.renameUser(oldUsername, newUsername);
                updateUserDropdowns();
                updateEmailTable();
                updateUserTextField.setText("");
                updateUserPasswordField.setText("");
            }
        }
    }//GEN-LAST:event_updateUserButtonActionPerformed

    private void removeUserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeUserButtonActionPerformed
        String username = (String) removeUserDropdown.getSelectedItem();
        mc.disableUser(username);
        updateUserDropdowns();
    }//GEN-LAST:event_removeUserButtonActionPerformed

    private void addUserButtonFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_addUserButtonFocusLost
        usersMessage.setText("");
    }//GEN-LAST:event_addUserButtonFocusLost

    private void addUserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addUserButtonActionPerformed
        String username = addUserTextField.getText();
        String pw = "";
        char[] password = addUserPasswordField.getPassword();
        for (char c : password) {
            pw += c;
        }
        username = username.trim();
        pw = pw.trim();
        if (username.length() == 0 || pw.length() == 0) {
            usersMessage.setText("ERROR: username and/or password cannot be blank.");
        } else if (mc.isUserEnabled(username)) {
            usersMessage.setText("ERROR: user already exists with that name.");
        } else if (!Utilities.isValidUsername(username)) {
            usersMessage.setText("ERROR: username must begin with a letter and contain only letters and numbers.");
        } else if (!Utilities.isValidPassword(pw)) {
            usersMessage.setText("ERROR: password must contain at least 4 characters with no whitespaces.");
        } else {
            mc.addUser(username, pw);
            addUserTextField.setText("");
            addUserPasswordField.setText("");
            updateUserDropdowns();
        }
    }//GEN-LAST:event_addUserButtonActionPerformed

    private void allowEmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allowEmailActionPerformed
        String addr = (String) emailAddressDropdown.getSelectedItem();
        String usr = (String) emailUserDropdown.getSelectedItem();

        if (mc.isEmailAlreadyAdded(addr) && mc.isUserEnabled(usr)) {
            mc.setEmailUser(addr, usr);
        }
        updateEmailTable();
    }//GEN-LAST:event_allowEmailActionPerformed

    private void blockEmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blockEmailActionPerformed
        String addr = (String) emailAddressDropdown.getSelectedItem();
        mc.blockEmail(addr);
        updateEmailTable();
    }//GEN-LAST:event_blockEmailActionPerformed

    private void mergeEnvelopesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mergeEnvelopesButtonActionPerformed
        String mergeFromEnvName = (String) transEnvelopeDropdown.getSelectedItem();
        String mergeToEnvName = (String) mergeEnvelopesList.getSelectedItem();

        if (mergeFromEnvName != null && mergeToEnvName != null && !mergeFromEnvName.equalsIgnoreCase(mergeToEnvName) && !mergeFromEnvName.equalsIgnoreCase(ALL)) {
            // give user second chance to back out of the merger
            String msg = "WARNING: merging two envelopes CANNOT be undone.\n"
                    + "Are you sure you want to move all transactions\n"
                    + "from '" + mergeFromEnvName + "' into '" + mergeToEnvName + "',\n"
                    + "and then remove '" + mergeFromEnvName + "'?";
            String title = "Merge Envelopes";
            int yes = 0;
            int opt = JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt == yes) {
                mc.mergeEnvelopes(mergeFromEnvName, mergeToEnvName);
                updateEnvelopeTable();
                updateEnvelopeDropdowns();
            }
        }
    }//GEN-LAST:event_mergeEnvelopesButtonActionPerformed

    private void setCategoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setCategoryButtonActionPerformed
        String envName = (String) transEnvelopeDropdown.getSelectedItem();
        String catName = (String) transCategoryDropdown.getSelectedItem();
        if (envName != null && catName != null && !envName.equalsIgnoreCase(ALL)) {
            if(catName.equalsIgnoreCase(NONE)) {
                mc.setEnvelopeCategory(envName, UNCAT);
            } else {
                mc.setEnvelopeCategory(envName, catName);
            }
            updateEnvelopeTable();
            updateTransactionTable();
        }
    }//GEN-LAST:event_setCategoryButtonActionPerformed

    private void removeCategoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeCategoryButtonActionPerformed
        String catName = (String) transCategoryDropdown.getSelectedItem();
        if (catName != null && !catName.equalsIgnoreCase(NONE)) {
            mc.removeCategory(catName);
            updateEnvelopeTable();
            updateCategoryDropdowns();
        }
    }//GEN-LAST:event_removeCategoryButtonActionPerformed

    private void removeCategoryButtonFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_removeCategoryButtonFocusLost
        message.setText("");
    }//GEN-LAST:event_removeCategoryButtonFocusLost

    private void removeAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAccountButtonActionPerformed
        String acctName = (String) transAccountDropdown.getSelectedItem();
        if (acctName != null && !acctName.equalsIgnoreCase(ALL)) {
            if (mc.disableAccount(acctName)) {
                updateAccountTable();
                updateAccountDropdowns();
            } else {
                message.setText("ERROR: '" + acctName + "' must have a zero balance before it can be removed");
            }
        }
    }//GEN-LAST:event_removeAccountButtonActionPerformed

    private void removeAccountButtonFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_removeAccountButtonFocusLost
        message.setText("");
    }//GEN-LAST:event_removeAccountButtonFocusLost

    private void removeEnvelopeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeEnvelopeButtonActionPerformed
        String envName = (String) transEnvelopeDropdown.getSelectedItem();
        if (envName != null && !envName.equalsIgnoreCase(ALL)) {
            if (mc.removeEnvelope(envName)) {
                updateEnvelopeTable();
                updateEnvelopeDropdowns();
            } else {
                message.setText("ERROR: '" + envName + "' must not have any transactions before it can be removed; please move transactions manually or by using the merge button");
            }
        }
    }//GEN-LAST:event_removeEnvelopeButtonActionPerformed

    private void removeEnvelopeButtonFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_removeEnvelopeButtonFocusLost
        message.setText("");
    }//GEN-LAST:event_removeEnvelopeButtonFocusLost

    private void hideTransfersToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideTransfersToggleButtonActionPerformed
        validateTransactionFields();
        updateSelected();
    }//GEN-LAST:event_hideTransfersToggleButtonActionPerformed

    private void transactionsRefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transactionsRefreshButtonActionPerformed
        validateTransactionFields();
        updateSelected();
    }//GEN-LAST:event_transactionsRefreshButtonActionPerformed

    private void envTransferButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_envTransferButtonActionPerformed
        String date = transactionDateField.getText();
        String desc = transactionDescriptionField.getText();
        String amt = transactionAmtField.getText();
        String from = (String) envTransferFrom.getSelectedItem();
        String to = (String) envTransferTo.getSelectedItem();

        if (Utilities.isDate(date)
                && Utilities.isValidDescription(desc)
                && Utilities.isValidAmount(amt)
                && !from.equalsIgnoreCase(to)
                && !currUser.equalsIgnoreCase("")) {
            mc.addTransfer(date, desc, amt, from, to, currUser);
            updateEnvelopeTable();
            updateTransactionTable();
            transactionAmtField.setText("");
            transactionDescriptionField.setText("");
            message.setText("");
        } else {
            message.setText("ERROR: transfer not completed");
        }
    }//GEN-LAST:event_envTransferButtonActionPerformed

    private void acctTransferButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acctTransferButtonActionPerformed
        String date = transactionDateField.getText();
        String desc = transactionDescriptionField.getText();
        String amt = transactionAmtField.getText();
        String from = (String) acctTransferFrom.getSelectedItem();
        String to = (String) acctTransferTo.getSelectedItem();

        if (Utilities.isDate(date)
                && Utilities.isValidDescription(desc)
                && Utilities.isValidAmount(amt)
                && !from.equalsIgnoreCase(to)
                && !currUser.equalsIgnoreCase("")) {
            mc.addTransfer(date, desc, amt, from, to, currUser);
            updateAccountTable();
            updateTransactionTable();
            transactionAmtField.setText("");
            transactionDescriptionField.setText("");
            message.setText("");
        } else {
            message.setText("ERROR: transfer not completed");
        }
    }//GEN-LAST:event_acctTransferButtonActionPerformed

    private void newTransactionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newTransactionButtonActionPerformed
        String date = transactionDateField.getText();
        String desc = transactionDescriptionField.getText();
        String amt = transactionAmtField.getText();
        String acct = (String) newTransAcctDropdown.getSelectedItem();
        String env = (String) newTransEnvDropdown.getSelectedItem();

        if (Utilities.isDate(date)
                && Utilities.isValidDescription(desc)
                && Utilities.isValidAmount(amt)
                && !currUser.equalsIgnoreCase("")) {
            mc.addTransaction(date, desc, amt, acct, currUser, env);
            updateAccountTable();
            updateEnvelopeTable();
            updateTransactionTable();
            transactionDescriptionField.setText("");
            transactionAmtField.setText("");
            message.setText("");
        } else {
            message.setText("ERROR: transaction not added");
        }
    }//GEN-LAST:event_newTransactionButtonActionPerformed

    private void transactionAmtFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_transactionAmtFieldFocusLost
        if (transactionAmtField.getText().length() > 0) {
            try {
                int amtInt = Utilities.amountToInteger(transactionAmtField.getText());
                transactionAmtField.setText(Utilities.amountToStringSimple(amtInt));
            } catch (Exception ex) {
                addErrorMsg("amount must be in the form of a decimal");
                transactionAmtField.setForeground(Color.RED);
            }
        }
    }//GEN-LAST:event_transactionAmtFieldFocusLost

    private void transactionAmtFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_transactionAmtFieldFocusGained
        removeErrorMsg("amount must be in the form of a decimal");
        transactionAmtField.setForeground(Color.BLACK);
    }//GEN-LAST:event_transactionAmtFieldFocusGained

    private void transactionDescriptionFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_transactionDescriptionFieldFocusLost
        if (!Utilities.isValidDescription(transactionDescriptionField.getText())) {
            addErrorMsg("max 100 characters for desc and must contain only letters, numbers, and/or the following: - ( ) @ # $ % &");
            transactionDescriptionField.setForeground(Color.RED);
        }
    }//GEN-LAST:event_transactionDescriptionFieldFocusLost

    private void transactionDescriptionFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_transactionDescriptionFieldFocusGained
        removeErrorMsg("max 100 characters for desc and must contain only letters, numbers, and/or the following: - ( ) @ # $ % &");
        transactionDescriptionField.setForeground(Color.BLACK);
    }//GEN-LAST:event_transactionDescriptionFieldFocusGained

    private void transactionDateFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_transactionDateFieldFocusLost
        validateTransactionFields();
    }//GEN-LAST:event_transactionDateFieldFocusLost

    private void exportTransactionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportTransactionsButtonActionPerformed
        JFileChooser fc = new JFileChooser();

        String acctSelected = "_" + (String) transAccountDropdown.getSelectedItem();
        String envSelected = "_" + (String) transEnvelopeDropdown.getSelectedItem();

        if (acctSelected.equalsIgnoreCase("_-all-")) {
            acctSelected = "";
        }
        if (envSelected.equalsIgnoreCase("_-all-")) {
            envSelected = "";
        }
        String tmpName = acctSelected + envSelected;
        if (tmpName.length() == 0) {
            fc.setSelectedFile(new File(Utilities.getDatestamp(0) + "_transactions"));
        } else {
            fc.setSelectedFile(new File(Utilities.getDatestamp(0) + "_transactions" + tmpName));
        }
        int returnVal = fc.showSaveDialog(this);
        String fileName;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            fileName = file.getAbsolutePath();
            transactionsTM.export(fileName);
        }
    }//GEN-LAST:event_exportTransactionsButtonActionPerformed

    private void transEnvelopeDropdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transEnvelopeDropdownActionPerformed
        updateSelected();
    }//GEN-LAST:event_transEnvelopeDropdownActionPerformed

    private void transAccountDropdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transAccountDropdownActionPerformed
        updateSelected();
    }//GEN-LAST:event_transAccountDropdownActionPerformed

    private void dateRangeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dateRangeCheckBoxActionPerformed
        if (dateRangeCheckBox.isSelected()) {
            transFromField.setEnabled(true);
            transToField.setEnabled(true);
            transactionQtyTextField.setEnabled(false);
        } else {
            transFromField.setEnabled(false);
            transToField.setEnabled(false);
            transactionQtyTextField.setEnabled(true);
        }
        validateTransactionFields();
        updateSelected();
    }//GEN-LAST:event_dateRangeCheckBoxActionPerformed

    private void addCategoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addCategoryButtonActionPerformed
        // get name from textfield
        String newCatName = newCategoryField.getText().toLowerCase();
        newCatName = newCatName.trim();
        // only take action if textfield is not empty
        if (newCatName.length() > 0) {
            if (mc.addCategory(newCatName)) {
                updateEnvelopeTable();
                updateCategoryDropdowns();
                newCategoryField.setText("");
            } else {
                message.setText("ERROR: could not create category '" + newCatName + "', try a different name.");
                newCategoryField.setForeground(Color.RED);
            }
        } else {
            message.setText("ERROR: name cannot be blank");
        }
    }//GEN-LAST:event_addCategoryButtonActionPerformed

    private void addCategoryButtonFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_addCategoryButtonFocusLost
        message.setText("");
        newCategoryField.setForeground(Color.BLACK);
    }//GEN-LAST:event_addCategoryButtonFocusLost

    private void newCategoryFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_newCategoryFieldKeyPressed
        if (evt.getKeyCode() == 10) { // <enter> is pressed
            // get name from textfield
            String newCatName = newCategoryField.getText().toLowerCase();
            newCatName = newCatName.trim();
            // only take action if textfield is not empty
            if (newCatName.length() > 0) {
                if (mc.addCategory(newCatName)) {
                    updateEnvelopeTable();
                    updateCategoryDropdowns();
                    newCategoryField.setText("");
                } else {
                    message.setText("ERROR: could not create category '" + newCatName + "', try a different name.");
                    newCategoryField.setForeground(Color.RED);
                }
            } else {
                message.setText("ERROR: name cannot be blank");
            }
        } else {
            newCategoryField.setForeground(Color.BLACK);
            message.setText("");
        }
    }//GEN-LAST:event_newCategoryFieldKeyPressed

    private void addEnvelopeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addEnvelopeButtonActionPerformed
        // get name from textfield
        String newEnvName = newEnvelopeField.getText().toLowerCase();
        newEnvName = newEnvName.trim();
        // only take action if textfield is not empty
        if (newEnvName.length() > 0) {
            if (mc.addEnvelope(newEnvName)) {
                updateEnvelopeTable();
                updateEnvelopeDropdowns();
                newEnvelopeField.setText("");
            } else {
                message.setText("ERROR: could not create envelope '" + newEnvName + "', try a different name.");
                newEnvelopeField.setForeground(Color.RED);
            }
        } else {
            message.setText("ERROR: name cannot be blank");
        }
    }//GEN-LAST:event_addEnvelopeButtonActionPerformed

    private void addEnvelopeButtonFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_addEnvelopeButtonFocusLost
        message.setText("");
        newEnvelopeField.setForeground(Color.BLACK);
    }//GEN-LAST:event_addEnvelopeButtonFocusLost

    private void newEnvelopeFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_newEnvelopeFieldKeyPressed
        if (evt.getKeyCode() == 10) { // <enter> is pressed
            // get name from textfield
            String newEnvName = newEnvelopeField.getText().toLowerCase();
            newEnvName = newEnvName.trim();
            // only take action if textfield is not empty
            if (newEnvName.length() > 0) {
                if (mc.addEnvelope(newEnvName)) {
                    updateEnvelopeTable();
                    updateEnvelopeDropdowns();
                    newEnvelopeField.setText("");
                } else {
                    message.setText("ERROR: could not create envelope '" + newEnvName + "', try a different name.");
                    newEnvelopeField.setForeground(Color.RED);
                }
            } else {
                message.setText("ERROR: name cannot be blank");
            }
        } else {
            newEnvelopeField.setForeground(Color.BLACK);
            message.setText("");
        }
    }//GEN-LAST:event_newEnvelopeFieldKeyPressed

    private void categorizedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_categorizedCheckBoxActionPerformed
        mc.setCategorized(categorizedCheckBox.isSelected());
        updateEnvelopeTable();
        envelopesTable.updateUI();
    }//GEN-LAST:event_categorizedCheckBoxActionPerformed

    private void envelopesTablePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_envelopesTablePropertyChange
        if (evt.getPropertyName().equalsIgnoreCase("tableCellEditor") && evt.getNewValue() == null) {
            updateEnvelopeDropdowns();
            updateEnvelopeTable();
            updateCategoryDropdowns();
        }
    }//GEN-LAST:event_envelopesTablePropertyChange

    private void addAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAccountButtonActionPerformed
        // get name from textfield
        String newAcctName = newAccountField.getText().toLowerCase();
        newAcctName = newAcctName.trim();
        // only take action if textfield is not empty
        if (newAcctName.length() > 0) {
            if (mc.addAccount(newAcctName)) {
                updateAccountTable();
                updateAccountDropdowns();
                newAccountField.setText("");
            } else {
                message.setText("ERROR: could not create account '" + newAcctName + "', try a different name.");
                newAccountField.setForeground(Color.RED);
            }
        } else {
            message.setText("ERROR: name cannot be blank");
        }
    }//GEN-LAST:event_addAccountButtonActionPerformed

    private void addAccountButtonFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_addAccountButtonFocusLost
        message.setText("");
        newAccountField.setForeground(Color.BLACK);
    }//GEN-LAST:event_addAccountButtonFocusLost

    private void newAccountFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_newAccountFieldKeyPressed
        if (evt.getKeyCode() == 10) { // <enter> is pressed
            // get name from textfield
            String newAcctName = newAccountField.getText().toLowerCase();
            newAcctName = newAcctName.trim();
            // only take action if textfield is not empty
            if (newAcctName.length() > 0) {
                if (mc.addAccount(newAcctName)) {
                    updateAccountTable();
                    updateAccountDropdowns();
                    newAccountField.setText("");
                } else {
                    message.setText("ERROR: could not create account '" + newAcctName + "', try a different name.");
                    newAccountField.setForeground(Color.RED);
                }
            } else {
                message.setText("ERROR: name cannot be blank");
            }
        } else {
            newAccountField.setForeground(Color.BLACK);
            message.setText("");
        }
    }//GEN-LAST:event_newAccountFieldKeyPressed

    private void accountsTablePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_accountsTablePropertyChange
        if (evt.getPropertyName().equalsIgnoreCase("tableCellEditor") && evt.getNewValue() == null) {
            updateAccountDropdowns();
            updateAccountTable();
        }
    }//GEN-LAST:event_accountsTablePropertyChange

    private void transactionQtyTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_transactionQtyTextFieldKeyPressed
        if (evt.getKeyCode() == 10) { // <enter> is pressed
            validateTransactionFields();
            updateSelected();
        }
    }//GEN-LAST:event_transactionQtyTextFieldKeyPressed

    private void transactionsTablePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_transactionsTablePropertyChange
        if (evt.getPropertyName().equalsIgnoreCase("tableCellEditor") && evt.getNewValue()==null) {
            updateSelected();
            updateEnvelopeTable();
            updateAccountTable();
        }
    }//GEN-LAST:event_transactionsTablePropertyChange

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Console.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Console().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane AccountsScrollPane;
    private javax.swing.JScrollPane EnvelopesScrollPane;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JPanel accountManagementPane;
    public javax.swing.JTable accountsTable;
    private javax.swing.JButton acctTransferButton;
    public javax.swing.JComboBox acctTransferFrom;
    public javax.swing.JComboBox acctTransferTo;
    private javax.swing.JButton addAccountButton;
    private javax.swing.JButton addCategoryButton;
    private javax.swing.JButton addEnvelopeButton;
    private javax.swing.JButton addUserButton;
    private javax.swing.JPasswordField addUserPasswordField;
    private javax.swing.JTextField addUserTextField;
    private javax.swing.JPanel adminTab;
    private javax.swing.JButton allTransactionsButton;
    private javax.swing.JButton allowEmail;
    private javax.swing.JLabel amountLabel;
    private javax.swing.JButton blockEmail;
    private javax.swing.JButton budgetWorksheetButton;
    public javax.swing.JCheckBox categorizedCheckBox;
    private javax.swing.JLabel dateLabel;
    public javax.swing.JCheckBox dateRangeCheckBox;
    public javax.swing.JComboBox emailAddressDropdown;
    private javax.swing.JPanel emailPane;
    public javax.swing.JTable emailTable;
    public javax.swing.JComboBox emailUserDropdown;
    private javax.swing.JButton envTransferButton;
    public javax.swing.JComboBox envTransferFrom;
    public javax.swing.JComboBox envTransferTo;
    private javax.swing.JPanel envelopesTab;
    public javax.swing.JTable envelopesTable;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JButton exportTransactionsButton;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel fromLabel;
    private javax.swing.JPasswordField gmailPassword;
    private javax.swing.JLabel gmailServerStatus;
    private javax.swing.JTextField gmailUsername;
    public javax.swing.JCheckBox hideTransfersToggleButton;
    private javax.swing.JTextField intervalCountTextField;
    private javax.swing.JLabel intervalTagLabel;
    private javax.swing.JComboBox intervalTypeDropdown;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JLabel loginStatus;
    private javax.swing.JToggleButton loginToggleButton;
    public javax.swing.JComboBox loginUserDropdown;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton mergeEnvelopesButton;
    public javax.swing.JComboBox mergeEnvelopesList;
    private javax.swing.JLabel message;
    private javax.swing.JTextField newAccountField;
    private javax.swing.JTextField newCategoryField;
    private javax.swing.JTextField newEnvelopeField;
    public javax.swing.JComboBox newTransAcctDropdown;
    public javax.swing.JComboBox newTransEnvDropdown;
    private javax.swing.JButton newTransactionButton;
    private javax.swing.JButton removeAccountButton;
    private javax.swing.JButton removeCategoryButton;
    private javax.swing.JButton removeEnvelopeButton;
    private javax.swing.JButton removeUserButton;
    public javax.swing.JComboBox removeUserDropdown;
    private javax.swing.JProgressBar reportProgressBar;
    private javax.swing.JPanel reportsPanel;
    private javax.swing.JMenuItem resetDatabaseMenuItem;
    private javax.swing.JButton runTrendReportButton;
    private javax.swing.JLabel selectedAcctAmt;
    private javax.swing.JLabel selectedAcctAmtLabel;
    private javax.swing.JLabel selectedEnvAmt;
    private javax.swing.JLabel selectedEnvAmtLabel;
    private javax.swing.JToggleButton serverToggleButton;
    private javax.swing.JButton setCategoryButton;
    private javax.swing.JButton snapshotButton;
    private javax.swing.JPanel summaryPanel;
    private javax.swing.JScrollPane summaryScrollPane;
    private javax.swing.JLabel toLabel;
    public javax.swing.JComboBox transAccountDropdown;
    public javax.swing.JComboBox transCategoryDropdown;
    public javax.swing.JComboBox transEnvelopeDropdown;
    public javax.swing.JTextField transFromField;
    public javax.swing.JTextField transToField;
    private javax.swing.JTextField transactionAmtField;
    private javax.swing.JTextField transactionDateField;
    private javax.swing.JTextField transactionDescriptionField;
    private javax.swing.JTextField transactionQtyTextField;
    private javax.swing.JLabel transactionsLabel;
    private javax.swing.JPanel transactionsPanel;
    private javax.swing.JButton transactionsRefreshButton;
    private javax.swing.JScrollPane transactionsScrollPane;
    public javax.swing.JTable transactionsTable;
    private javax.swing.JScrollPane transactionsTableScrollPane;
    private javax.swing.JLabel trendReportDirectionsLabel;
    private javax.swing.JTextField trendTextField;
    private javax.swing.JButton updateUserButton;
    public javax.swing.JComboBox updateUserDropdown;
    private javax.swing.JPasswordField updateUserPasswordField;
    private javax.swing.JTextField updateUserTextField;
    private javax.swing.JPasswordField userPassword;
    private javax.swing.JLabel usersMessage;
    // End of variables declaration//GEN-END:variables
}

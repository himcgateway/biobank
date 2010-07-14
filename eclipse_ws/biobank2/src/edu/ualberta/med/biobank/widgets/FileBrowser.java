package edu.ualberta.med.biobank.widgets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import edu.ualberta.med.biobank.BioBankPlugin;

public class FileBrowser extends BiobankWidget {
    private BiobankText textfield;
    private Button browse;
    private String text;

    public FileBrowser(Composite parent, int style) {
        super(parent, style);
        setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        setLayout(new GridLayout(2, true));
        GridData data = new GridData(GridData.FILL_BOTH);
        textfield = new BiobankText(this, SWT.NONE);
        textfield.setEditable(false);
        textfield.setLayoutData(data);
        browse = new Button(this, style);
        browse.setText("Browse");
        browse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fd = new FileDialog(FileBrowser.this.getShell(),
                    SWT.OPEN);
                fd.setText("Open");
                String[] filterExt = { "*.csv" };
                fd.setFilterExtensions(filterExt);
                String path = fd.open();
                if (path != null) {
                    textfield.setText(path);
                    File file = new File(path);
                    loadFile(file);
                }
            }
        });
        // browse.setLayoutData(data);
    }

    public void loadFile(File file) {
        if (file.isFile()) {
            String contents = "";
            FileReader fileReader;
            try {
                fileReader = new FileReader(file);
                BufferedReader br = new BufferedReader(fileReader);
                while (br.ready())
                    contents += br.readLine() + "\n";
            } catch (Exception e1) {
                BioBankPlugin.openError("IO Error", "Unable to read file.");
            }
            setText(contents);
        }
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        loadFile(new File(textfield.getText()));
        return text;
    }

}

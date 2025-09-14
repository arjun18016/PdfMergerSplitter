/*
Android application written in Java that allows users to merge multiple PDF files into a single PDF file
and split a single PDF file by extracting specific page ranges.

The code starts with package and import statements, which bring in necessary Android and third-party libraries
for PDF manipulation including AlertDialog for user input.

The MainActivity class extends AppCompatActivity, which is the base class for activities that use the support
library action bar features.

Member variables:
- REQUEST_PICK_PDF_FILES: A constant integer used as a request code when selecting multiple PDF files for merging.
- REQUEST_PICK_PDF_FILE_FOR_SPLIT: A constant integer used as a request code when selecting a single PDF file for splitting.
- selectPDF: A button used to trigger the PDF selection process for merging multiple files.
- mergePDF: A button used to initiate the merging process of selected PDF files.
- selectPDFSplit: A button used to select a single PDF file for splitting operation.
- splitPDF: A button used to initiate the splitting process with user-specified page ranges.
- mSelectedPdfs: A list to store the URIs of multiple selected PDF files for merging.
- mSelectedPdfForSplit: A URI to store the single selected PDF file for splitting.

In the onCreate method, the layout is set using setContentView(R.layout.activity_main).

Four click listeners are assigned to the buttons:

1. selectPDF button listener:
   - Clears any previous split selection to prevent cross-contamination
   - Opens the file picker to allow the user to select multiple PDF files for merging
   - Uses EXTRA_ALLOW_MULTIPLE flag for multiple file selection

2. mergePDF button listener:
   - Validates that PDF files are selected for merging
   - Creates a new timestamped PDF file in the Documents directory
   - Merges all selected PDF files into a single document using iTextPDF library
   - Clears the selection after successful merge
   - Opens the merged PDF file automatically

3. selectPDFSplit button listener:
   - Clears any previous merge selections to prevent cross-contamination
   - Opens the file picker to allow selection of a single PDF file for splitting
   - Does not use EXTRA_ALLOW_MULTIPLE flag for single file selection

4. splitPDF button listener:
   - Validates that a PDF file is selected for splitting
   - Shows an AlertDialog asking user to input page ranges (e.g., "1-3,5,7-9")
   - Parses the input to extract specified pages from the source PDF
   - Creates a new PDF containing only the requested pages
   - Validates page numbers against the source PDF's total page count
   - Clears the selection after successful split
   - Opens the resulting split PDF file automatically

When the user selects PDF files in the file picker, the onActivityResult method is called:
- For merge operation: Selected URIs are stored in the mSelectedPdfs list
- For split operation: Selected URI is stored in mSelectedPdfForSplit variable
- Toast messages display the number of selected files

Cross-contamination prevention:
- Selecting files for merge clears any previous split selection
- Selecting file for split clears any previous merge selections
- Successful operations clear their respective selections
- This ensures clean state transitions between different operations

Error handling includes:
- Validation of file selections before operations
- Page range validation for split operations
- User-friendly error messages for invalid inputs
- Exception handling during PDF processing operations

The application uses FileProvider for secure file sharing and opens resulting PDF files
in the default PDF viewer application.
*/


package com.arjun.pdfmergespilt;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_PDF_FILES = 1;
    private static final int REQUEST_PICK_PDF_FILE_FOR_SPLIT = 2;
    private Button selectPDF;
    private Button mergePDF;
    private Button selectPDFSplit;
    private Button splitPDF;
    private List<Uri> mSelectedPdfs = new ArrayList<>();
    private Uri mSelectedPdfForSplit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectPDF = findViewById(R.id.select_pdf_button);
        mergePDF = findViewById(R.id.merge_pdf_button);
        selectPDFSplit = findViewById(R.id.select_pdf_split_button);
        splitPDF = findViewById(R.id.split_pdf_button);

        selectPDF.setOnClickListener(new View.OnClickListener() {
            @Override //Open document dialog to select pdf files
            public void onClick(View view) {
                // Clear split selection when selecting files for merge
                mSelectedPdfForSplit = null;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
                startActivityForResult(intent, REQUEST_PICK_PDF_FILES);
            }
        });

        mergePDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSelectedPdfs.isEmpty()){
                    Toast.makeText(MainActivity.this, "Please select file!", Toast.LENGTH_SHORT).show();
                    return;
                }

                try{
                    //Create new pdf file into which we will merge
                    String filename = System.currentTimeMillis() + "_merged.pdf";
                    File mergedPdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),filename);
                    FileOutputStream outputStream = new FileOutputStream(mergedPdfFile);
                    Document document = new Document();
                    PdfCopy copy = new PdfCopy(document,outputStream);
                    document.open();
                    
                    //Add all files in new merged pdf file
                    for(Uri pdfUri: mSelectedPdfs){
                        PdfReader reader = new PdfReader(getContentResolver().openInputStream(pdfUri));
                        copy.addDocument(reader);
                        reader.close();
                    }
                    //Close and save new merged pdf file
                    document.close();
                    outputStream.close();

                    // Clear selections after successful merge
                    mSelectedPdfs.clear();

                    Toast.makeText(MainActivity.this,"PDF files merged successfully.", Toast.LENGTH_SHORT).show();
                    
                    //Open and display new merged pdf file
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri fileUri = FileProvider.getUriForFile(MainActivity.this,"com.arjun.pdfmergespilt.provider", mergedPdfFile);
                    intent.setDataAndType(fileUri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                }catch(Exception e){
                    e.printStackTrace();

                }
            }
        });

        selectPDFSplit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear merge selections when selecting file for split
                mSelectedPdfs.clear();

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/pdf");
                // Note: No EXTRA_ALLOW_MULTIPLE for single file selection
                startActivityForResult(intent, REQUEST_PICK_PDF_FILE_FOR_SPLIT);
            }
        });

        splitPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSelectedPdfForSplit == null){
                    Toast.makeText(MainActivity.this, "Please select a PDF file to split!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show dialog to get page range from user
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Split PDF by Page Range");

                // Set up the input
                final EditText input = new EditText(MainActivity.this);
                input.setHint("Enter page range (e.g., 1-3,5,7-9)");
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", (dialog, which) -> {
                    String rangeInput = input.getText().toString().trim();
                    if (rangeInput.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Please enter a page range!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        PdfReader reader = new PdfReader(getContentResolver().openInputStream(mSelectedPdfForSplit));
                        int numberOfPages = reader.getNumberOfPages();

                        // Create a single PDF file with selected pages
                        String filename = System.currentTimeMillis() + "_split.pdf";
                        File splitPdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);
                        FileOutputStream outputStream = new FileOutputStream(splitPdfFile);

                        Document document = new Document();
                        PdfCopy copy = new PdfCopy(document, outputStream);
                        document.open();

                        // Parse ranges and add pages to the single PDF
                        String[] ranges = rangeInput.split(",");
                        int totalPagesAdded = 0;

                        for (String range : ranges) {
                            range = range.trim();
                            String[] endpoints = range.split("-");
                            int start = Integer.parseInt(endpoints[0].trim());
                            int end = (endpoints.length > 1) ? Integer.parseInt(endpoints[1].trim()) : start;

                            // Validate page numbers
                            if (start < 1 || start > numberOfPages || end < 1 || end > numberOfPages) {
                                Toast.makeText(MainActivity.this, "Invalid page range: " + range + ". PDF has " + numberOfPages + " pages.", Toast.LENGTH_SHORT).show();
                                continue;
                            }

                            // Add pages from start to end
                            for (int i = start; i <= end; i++) {
                                copy.addPage(copy.getImportedPage(reader, i));
                                totalPagesAdded++;
                            }
                        }

                        // Close and save the PDF
                        document.close();
                        outputStream.close();
                        reader.close();

                        if (totalPagesAdded > 0) {
                            // Clear split selection after successful split
                            mSelectedPdfForSplit = null;

                            Toast.makeText(MainActivity.this, "PDF split successfully! " + totalPagesAdded + " pages extracted.", Toast.LENGTH_SHORT).show();

                            // Open the newly created PDF file
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri fileUri = FileProvider.getUriForFile(MainActivity.this, "com.arjun.pdfmergespilt.provider", splitPdfFile);
                            intent.setDataAndType(fileUri, "application/pdf");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "No valid pages found to extract!", Toast.LENGTH_SHORT).show();
                        }

                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Invalid page range format! Use format like: 1-3,5,7-9", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error splitting PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                builder.show();
            }
        });
    }

    @Override 
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Display message with number of selected files
        if(requestCode == REQUEST_PICK_PDF_FILES && resultCode == RESULT_OK){
            mSelectedPdfs.clear();
            if(data.getData() != null){
                mSelectedPdfs.add(data.getData());
            }else if(data.getClipData() != null){
                for(int i=0; i<data.getClipData().getItemCount(); i++){
                    mSelectedPdfs.add(data.getClipData().getItemAt(i).getUri());
                }
            }
            Toast.makeText(this,"Selected " + mSelectedPdfs.size() + " PDF Files.", Toast.LENGTH_SHORT).show();

        }else if(requestCode == REQUEST_PICK_PDF_FILE_FOR_SPLIT && resultCode == RESULT_OK){
            if(data.getData() != null){
                mSelectedPdfForSplit = data.getData();
                Toast.makeText(this,"Selected PDF File for Split.", Toast.LENGTH_SHORT).show();
            }
        }

    }
}

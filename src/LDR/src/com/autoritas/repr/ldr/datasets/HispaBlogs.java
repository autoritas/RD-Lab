/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autoritas.repr.ldr.datasets;

import com.autoritas.repr.ldr.GenerateArff;
import com.autoritas.repr.ldr.Prepare;
import com.autoritas.repr.ldr.Tools;
import com.autoritas.repr.ldr.iLoadDocsxClass;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Francisco Rangel (@kicorangel)
 */
public class HispaBlogs implements iLoadDocsxClass {

    private int mMinFreq;
    private int mMinSize;
    private String msCorpusPath;
    private String msLDRPath;
    private String msTrainingArff;
    private String msTestPath;
    private String msTestArff;
    
    public HispaBlogs(int minFreq, int minSize, String corpusPath, String LDRPath, String trainingArff, String testPath, String testArff) {
        mMinFreq = minFreq;
        mMinSize = minSize;
        msCorpusPath = corpusPath;
        msLDRPath = LDRPath;
        msTrainingArff = trainingArff;
        msTestPath = testPath;
        msTestArff = testArff;
    }
    
    
    public void Run() throws IOException, FileNotFoundException, ParserConfigurationException, SAXException {
        ArrayList<String> Labels = GetLabels();
        // Step 1: Prepare LDR weights and probabilities
        {
            Prepare oPrepare = new Prepare(mMinFreq, mMinSize, Labels, this, msCorpusPath, msLDRPath, true);
            oPrepare.Process();
        }   
        
        // Step 2: Generate training ARFF
        {
            GenerateArff oGenerate = new GenerateArff(mMinFreq, mMinSize, Labels,  this, msCorpusPath, msLDRPath, "", msTrainingArff, true);
            oGenerate.Process();
        }
        
        // Step 3: Generate test ARFF
        {
            GenerateArff oGenerate = new GenerateArff(mMinFreq, mMinSize, Labels, this, msCorpusPath, msLDRPath, msTestPath, msTestArff, true);
            oGenerate.Process();
        }
    }
    
    public ArrayList<String> GetLabels() {
        ArrayList<String> Labels = new ArrayList<String>(); 
        Labels.add("ar");
        Labels.add("cl");
        Labels.add("es");
        Labels.add("mx");
        Labels.add("pe");
        
        return Labels;
    }
    
    public Hashtable<String, Hashtable<String, Integer>> LoadDocsxClass(String sData, String label, int minSize, boolean verbose) {
        Hashtable<String, Hashtable<String, Integer>> oDocs = new Hashtable<String, Hashtable<String, Integer>>();

        File directory = new File(sData + "/" + label.toLowerCase() + "/");
        String[] files = directory.list();
        for (int iFile = 0; iFile < files.length; iFile++) {
            if (verbose) {
                System.out.println(label + ": " + (iFile + 1) + "/" + files.length);
            }
            try {
                String sFileName = files[iFile];
                String[] fileInfo = sFileName.split("_");
                String sAuthor = fileInfo[0];

                File fXmlFile = new File(sData + "/" + label.toLowerCase() + "/" + files[iFile]);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(fXmlFile);
                NodeList documents = doc.getDocumentElement().getElementsByTagName("document");
                for (int i = 0; i < documents.getLength(); i++) {
                    try {
                        Element element = (Element) documents.item(i);
                        String sHtmlContent = element.getTextContent();
                        String sContent = Tools.GetText(sHtmlContent);
                        sContent = Tools.Prepare(sContent);

                        String[] tokens = sContent.split(" ");
                        for (String t : tokens) {
                            String sLemma = t.toLowerCase().trim();

                            if (sLemma.length() < minSize) {
                                continue;
                            }
                            Hashtable<String, Integer> oFreq = new Hashtable<String, Integer>();
                            if (oDocs.containsKey(sAuthor)) {
                                oFreq = oDocs.get(sAuthor);
                            }
                            int iFreq = 0;
                            if (oFreq.containsKey(sLemma)) {
                                iFreq = oFreq.get(sLemma);
                            }
                            oFreq.put(sLemma, ++iFreq);
                            oDocs.put(sAuthor, oFreq);
                        }
                    } catch (Exception ex) {

                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.toString());
            }
        }

        return oDocs;
    }
}
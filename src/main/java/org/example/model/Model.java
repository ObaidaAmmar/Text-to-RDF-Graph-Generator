package org.example.model;
import org.example.common.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;


public class Model {

    private File uploadedFile;
    private LinkedHashMap<String, ArrayList<Pair<String,String>>> relations;
    public Model(File file)
    {
        uploadedFile = file;
        relations = new LinkedHashMap<>();
    }
    public Model()
    {relations = new LinkedHashMap<>();}

    public void setFile(File file)
    {uploadedFile = file;}
    public void setRelations(LinkedHashMap<String, ArrayList<Pair<String,String>>> rel)
    {relations = rel;}
    public File getFile()
    {return uploadedFile;}
    public LinkedHashMap<String, ArrayList<Pair<String,String>>> getRelations()
    {return relations;}


}

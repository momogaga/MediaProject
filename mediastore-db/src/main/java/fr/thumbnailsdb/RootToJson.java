package fr.thumbnailsdb;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author giuse_000
 */
public class RootToJson {

    String initialpath;
    Boolean recursivePath;
    public int filecount = 0;
    public int dircount = 0;
    JSONArray json = new JSONArray();

    public JSONArray getJson() {
        return json;
    }
    JSONArray children = new JSONArray();

    public RootToJson(String path, Boolean subFolder) {
        super();
        this.initialpath = path;
        this.recursivePath = subFolder;
    }

    public void list() throws JSONException {
        this.listDirectory(this.initialpath);
    }

    private void listDirectory(String dir) throws JSONException {
        JSONObject oFile;
        JSONObject oFolder;

        File file = new File(dir);
        File[] files = file.listFiles();
        if (files != null) {
            for (File file1 : files) {
                if (file1.isDirectory() == true) {
                    /*  [
                     {title: "Node 1", key: "1"},
                     {title: "Folder 2", key: "2", folder: true, children: [
                     {title: "Node 2.1", key: "3"},
                     {title: "Node 2.2", key: "4"}
                     ]}
                     ],*/
                    //System.out.println("Dossier" + files[i].getAbsolutePath());
                    oFolder = new JSONObject();
                    oFolder.put("title", file1.getName());
                    oFolder.put("folder", "true");
                    oFolder.put("children", children);
                    json.put(oFolder);
                    children = new JSONArray();

                    this.dircount++;
                } else {
                    oFile = new JSONObject();
                    oFile.put("title", file1.getName());
                    // System.out.println("Fichier" + files[i].getName());
                    if (file1.getParentFile().isDirectory() && !file1.getParent().equals(initialpath)) {
                        children.put(oFile);

                    } else {
                        json.put(oFile);
                        System.out.println(file1.getName());

                    }
                    this.filecount++;
                }
                if (file1.isDirectory() == true && this.recursivePath == true) {
                    this.listDirectory(file1.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Exemple : lister les fichiers dans tous les sous-dossiers
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        String pathToExplore = "C:\\Users\\giuse_000\\Pictures\\Photo Mailys";
        RootToJson diskFileExplorer = new RootToJson(pathToExplore, true);
        Long start = System.currentTimeMillis();
        diskFileExplorer.list();
        System.out.println("----------");
        System.out.println("Analyse de " + pathToExplore + " en " + (System.currentTimeMillis() - start) + " mses");
        System.out.println(diskFileExplorer.dircount + " dossiers");
        System.out.println(diskFileExplorer.filecount + " fichiers");
        System.out.println(diskFileExplorer.json);

    }

}

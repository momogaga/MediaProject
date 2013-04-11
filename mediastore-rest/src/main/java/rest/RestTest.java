package rest;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.sun.jersey.multipart.FormDataMultiPart;
import fr.thumbnailsdb.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import com.sun.jersey.multipart.BodyPartEntity;

import com.sun.jersey.spi.resource.Singleton;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("/hello")
@Singleton
public class RestTest {

    private final String dbFileName = "db.txt";

    String bdName;

    protected ThumbStore tb;
    protected SimilarImageFinder si;
    protected DuplicateMediaFinder df;


    DuplicateFolderList dc = null;


    public RestTest() {
        System.out.println("RestTest.RestTest()");

        File f = new File(dbFileName);
        if (f.exists()) {
            try {
                //TODO : Loop over lines
                BufferedReader fr = new BufferedReader(new FileReader(f));
                while ((bdName = fr.readLine()) != null) {
                    if (tb == null) {
                        tb = new ThumbStore(bdName);
                    } else {
                        tb.addDB(bdName);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To chanfge body of catch statement use File | Settings | File Templates.
            }
        } else {
            tb = new ThumbStore();
        }

        si = new SimilarImageFinder(tb);
        df = new DuplicateMediaFinder(tb);

    }


    @GET
    @Path("/db/{param}")
    public Response getDBInfo(@PathParam("param") String info) {
        System.out.println("RestTest.getDBInfo() " + info);
        if ("size".equals(info)) {
            System.out.println("RestTest.getDBInfo() " + tb.size());
            return Response.status(200).entity(tb.size() + "").build();
        }
        if ("path".equals(info)) {
            return Response.status(200).entity(tb.getPath() + "").build();
        }
        if ("status".equals(info)) {
            return Response.status(200).entity("idle").build();
        }
        // System.out.println("RestTest.getDBInfo() thumbstore is " + tb);
        return Response.status(404).build();
    }

    @GET
    @Path("/status")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getStatus() {
//        System.out.println("status " + Status.getStatus());
        return Response.status(200).entity(Status.getStatus()).build();

    }

    @GET
    @Path("/paths")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getPaths() {
//        System.out.println("status " + Status.getStatus());
        //  System.out.println("Found the pathsOfDBOnDisk " + tb.getIndexedPaths());
        return Response.status(200).entity(tb.getIndexedPaths()).build();

    }


    @GET
    @Path("/identical")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDuplicate(@QueryParam("max") String max, @QueryParam("folder") final java.util.List<String> obj) {
        System.out.println("RestTest.getDuplicate " + obj);
        Status.getStatus().setStringStatus("Requesting duplicate media");
        Collection dc = (Collection) df.computeDuplicateSets(df.findDuplicateMedia()).toCollection(Integer.parseInt(max), obj.toArray(new String[]{}));
        Status.getStatus().setStringStatus(Status.IDLE);
        return Response.status(200).entity(dc).build();
    }


    @GET
    @Path("/duplicateFolder")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDuplicateFolder(@QueryParam("folder") final java.util.List<String> obj) {
        System.out.println("RestTest.getDuplicateFolder " + obj);
        Status.getStatus().setStringStatus("Requesting duplicate folder list");

        Collection<DuplicateFolderGroup> col = getDuplicateFolderGroup().asSortedCollection(obj.toArray(new String[]{}), 300);
        System.out.println("RestTest.getDuplicateFolder sending results of size " + col.size());
        Status.getStatus().setStringStatus(Status.IDLE);

        return Response.status(200).entity(col).build();
    }

    private synchronized DuplicateFolderList getDuplicateFolderGroup() {

        if (dc == null) {
            dc = df.computeDuplicateFolderSets(df.findDuplicateMedia());
        }
        return dc;
    }

    @GET
    @Path("/duplicateFolderDetails")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDuplicateFolderDetails(@QueryParam("folder1") String f1, @QueryParam("folder2") String f2) {
        DuplicateFolderGroup group = getDuplicateFolderGroup().getDetails(f1, f2);
        JSONObject json = new JSONObject();

        try {
            json.put("file1", group.getFile1());
            json.put("file2", group.getFile2());
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        return Response.status(200).entity(json).build();
    }

    @GET
    @Path("getThumbnail/")
    @Produces("image/jpg")
    public Response getThumbnail(@QueryParam("path") String imageId) {
        System.out.println("Thubnail : imageID " + imageId);
        BufferedInputStream source = null;
        try {

            BufferedImage bf = ImageIO.read(new FileInputStream(new File(imageId)));


            // scale it to the new size on-the-fly
            BufferedImage thumbImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = thumbImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(bf, 0, 0, 100, 100, null);

            // save thumbnail image to outFilename
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(bout);
            ImageIO.write(thumbImage, "jpg", out);


            final byte[] imgData = bout.toByteArray();
            final InputStream bigInputStream =
                    new ByteArrayInputStream(imgData);
            return Response.status(200).entity(bigInputStream).build();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return Response.status(404).build();

    }


    protected String getImageAsHTMLImg(String imageId) {
        String img = "";
        try {
            File f = new File(imageId);
            img = "{\"data\" : \"" + Base64.encodeBase64String(FileUtils.readFileToByteArray(f)) + "\", \"title\" : \"" + f.getParent() + "\" }";

        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    @GET
    @Path("shrink/")
    public Response shrink(@QueryParam("folder") final java.util.List<String> obj) {
        tb.shrink(obj);
        return Response.status(200).entity("Shrink done").build();
    }


    @GET
    @Path("update/")
    public Response update(@QueryParam("folder") final java.util.List<String> obj) {
        new MediaIndexer(tb).updateDB(obj);
        return Response.status(200).entity("Update done").build();
    }

    @GET
    @Path("folder/")
    public Response getFolder(@QueryParam("path") String path) {
        System.out.println("RestTest.getFolder() input_path " + path);
        return Response.status(200).entity(path).type("application/folder").build();
    }

    @GET
    @Path("index/")
    public Response index(@QueryParam("path") String path) {
        System.out.println("RestTest.index() input_path " + path);
        return Response.status(200).entity("Indexing in progress").build();
    }


    @POST
    @Path("findSimilar/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findSimilar(FormDataMultiPart multipart) {
        ThumbnailGenerator tg = new ThumbnailGenerator(null);
        BodyPartEntity bpe = (BodyPartEntity) multipart.getBodyParts().get(0).getEntity();
        Collection<MediaFileDescriptor> c = null;
        ArrayList<SimilarImage> al = null;
        File temp = null;
        try {
            InputStream source = bpe.getInputStream();
            System.out.println("RestTest.findSimilar() received " + source);
            //BufferedImage bi = ImageIO.read(source);

            temp = File.createTempFile("tempImage", ".jpg");
            FileOutputStream fo = new FileOutputStream(temp);

            byte[] buffer = new byte[8 * 1024];

            int total = 0;
            try {
                int bytesRead;
                while ((bytesRead = source.read(buffer)) != -1) {
                    fo.write(buffer, 0, bytesRead);
                    total += bytesRead;
                }
            } finally {
                fo.close();
            }
            System.out.println("RestTest.findSimilar()  written to " + temp + " with size " + total);
        } catch (Exception e) {
            // message = e.getMessage();
            e.printStackTrace();
        }

        long t1 = System.currentTimeMillis();
        c = si.findSimilarMedia(temp.getAbsolutePath(), 10);
        long t2 = System.currentTimeMillis();
        System.out.println("Found similar files " + c.size() + " took " + (t2 - t1) + "ms");

        al = new ArrayList<SimilarImage>(c.size());
        for (MediaFileDescriptor mdf : c) {

            String path = mdf.getPath();

            String data = null;
            try {
                FileInputStream f = new FileInputStream(new File(path));
                try {
                    BufferedImage bf = tg.downScaleImage(ImageIO.read(f), 200, 200);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ImageIO.write(bf, "JPEG", out);
                    data = Base64.encodeBase64String(out.toByteArray());
                } finally {
                    f.close();
                }
                //  Base64.encodeBase64String(FileUtils.readFileToByteArray(new File(path)));
            } catch (IOException e) {
                System.err.println("Err: File " + path + " not found");
            }

            SimilarImage si = new SimilarImage(path, data, mdf.getRmse());
            al.add(si);
            System.out.println(si);

        }
        System.out.println("RestTest.findSimilar sending " + al.size() + " elements");

        JSONArray mJSONArray = new JSONArray();
        for (int i = 0; i < al.size(); i++) {
            JSONObject json = new JSONObject();
            try {
                json.put("path", al.get(i).path);
                json.put("base64Data", al.get(i).base64Data);
                json.put("rmse", al.get(i).rmse);
                mJSONArray.put(json);
            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        JSONObject responseDetailsJson = new JSONObject();
        try {
            responseDetailsJson.put("success", true);

            responseDetailsJson.put("images", mJSONArray);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return Response.status(200).entity(responseDetailsJson).type(MediaType.APPLICATION_JSON).build();
    }


    @POST
    @Path("findGPS/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findGPS(FormDataMultiPart multipart) {
        BodyPartEntity bpe = (BodyPartEntity) multipart.getBodyParts().get(0).getEntity();
        Collection<MediaFileDescriptor> c = null;
        ArrayList<SimilarImage> al = null;
        File temp = null;
        try {
            InputStream source = bpe.getInputStream();
            System.out.println("RestTest.findGPS() received " + source);

            temp = File.createTempFile("tempImage", ".jpg");
            FileOutputStream fo = new FileOutputStream(temp);

            byte[] buffer = new byte[8 * 1024];

            int total = 0;
            try {
                int bytesRead;
                while ((bytesRead = source.read(buffer)) != -1) {
                    fo.write(buffer, 0, bytesRead);
                    total += bytesRead;
                }
            } finally {
                fo.close();
            }
            System.out.println("RestTest.findGPS()  written to " + temp + " with size " + total);
        } catch (Exception e) {
            e.printStackTrace();
        }

        MetaDataFinder mdf = new MetaDataFinder(temp);
        double[] coo = mdf.getLatLong();

        JSONObject responseDetailsJson = new JSONObject();
        try {
            responseDetailsJson.put("lat", coo[0]);

            responseDetailsJson.put("lon", coo[1]);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        System.out.println("RestTest.findGPS sending json " + responseDetailsJson);
        return Response.status(200).entity(responseDetailsJson).type(MediaType.APPLICATION_JSON).build();
    }


    @GET
    @Path("findGPSFromPath/")
    @Produces({MediaType.APPLICATION_JSON})
    public Response findGPSFromPath(@QueryParam("path") String path) {
        System.out.println("RestTest.findGPSFromPath " + path);
        String rPath = null;
        try {
            rPath = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println("RestTest.findGPSFromPath real path " + rPath);
        File temp = new File(rPath);
        MetaDataFinder mdf = new MetaDataFinder(temp);
        double[] coo = mdf.getLatLong();
        JSONObject responseDetailsJson = new JSONObject();
        try {
            responseDetailsJson.put("lat", coo[0]);

            responseDetailsJson.put("lon", coo[1]);
            responseDetailsJson.put("date", mdf.getDate());
            responseDetailsJson.put("gps", mdf.getGPS());
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        System.out.println("RestTest.findGPSFromPath sending json " + responseDetailsJson);
        return Response.status(200).entity(responseDetailsJson).type(MediaType.APPLICATION_JSON).build();
    }


    @GET
    @Path("getAllGPS/")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAllGPS() {
        ArrayList<String> al = tb.getAllWithGPS();
        return Response.status(200).entity(al).type(MediaType.APPLICATION_JSON).build();
    }


    @XmlRootElement
    public class SimilarImage {
        @XmlElement
        public String path;
        @XmlElement
        public String base64Data;
        @XmlElement
        public double rmse;

        public SimilarImage(String path, String base64Data, double rmse) {
            this.rmse = rmse;
            this.path = path;
            this.base64Data = base64Data;
        }
    }


}
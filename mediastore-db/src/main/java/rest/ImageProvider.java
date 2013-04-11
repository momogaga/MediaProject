package rest; 

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes("image/jpeg")
class ImageProvider implements MessageBodyReader<Image> {

    public Image readFrom(Class<Image> type,
                                Type genericType,
                                Annotation[] annotations,
                                MediaType mediaType,
                                MultivaluedMap<String, String> httpHeaders,
                                InputStream entityStream) throws IOException,
        WebApplicationException {
       // create Image from stream
    	System.out.println("ImageProvider.readFrom()");
    	BufferedImage bf = ImageIO.read(entityStream);
    	return bf; 
    	
    }

	public boolean isReadable(Class<?> arg0, Type arg1, java.lang.annotation.Annotation[] arg2, MediaType arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	
}

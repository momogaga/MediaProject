import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;

public class Test {

    @BeforeMethod
    public void createTempDir() {
        File tmp = new File("tmp_test");

    }

    @AfterMethod
    public void deleteTempDir() { }


        @org.testng.annotations.Test
        public void sayHello() throws IOException {
            System.out.println("Hello " + new File("tototot-2").createNewFile());
            System.out.println("Hello " + new File("tototot-2").getAbsolutePath());
            assert true;
        }


}

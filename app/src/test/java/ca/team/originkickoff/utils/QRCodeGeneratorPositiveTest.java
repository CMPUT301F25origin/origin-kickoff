package ca.team.originkickoff.utils;

import android.graphics.Bitmap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Positive path tests for QRCodeGenerator using Robolectric to allow Bitmap creation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class QRCodeGeneratorPositiveTest {

    @Test
    public void generateQRCode_validContent_returnsBitmapOfExpectedSize() {
        Bitmap bmp = QRCodeGenerator.generateQRCode("EVENT-123");
        assertNotNull(bmp);
        assertEquals(512, bmp.getWidth());
        assertEquals(512, bmp.getHeight());
    }

    @Test
    public void bitmapToByteArray_validBitmap_returnsNonEmptyArray() {
        Bitmap bmp = QRCodeGenerator.generateQRCode("HELLO-WORLD");
        assertNotNull(bmp);
        byte[] data = QRCodeGenerator.bitmapToByteArray(bmp);
        assertNotNull(data);
        assertTrue("Expected PNG byte array to be non-empty", data.length > 50); // small threshold
    }
}


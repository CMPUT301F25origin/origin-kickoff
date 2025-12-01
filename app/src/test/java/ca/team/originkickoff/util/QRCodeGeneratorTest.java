package ca.team.originkickoff.util;

import ca.team.originkickoff.utils.QRCodeGenerator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for QRCodeGenerator covering quick, deterministic branches.
 * NOTE: Positive generation path (non-empty content) not tested here because it
 * requires android.graphics.Bitmap, which is unavailable in plain JVM tests.
 * For full coverage, use an instrumentation test or Robolectric.
 */
public class QRCodeGeneratorTest {

    @Test
    public void generateQRCode_nullContent_returnsNull() {
        assertNull(QRCodeGenerator.generateQRCode(null));
    }

    @Test
    public void generateQRCode_emptyContent_returnsNull() {
        assertNull(QRCodeGenerator.generateQRCode(""));
    }

    @Test
    public void bitmapToByteArray_nullBitmap_returnsNull() {
        assertNull(QRCodeGenerator.bitmapToByteArray(null));
    }
}

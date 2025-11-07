package ca.team.originkickoff;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ImageViewerActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_BITMAP = "image_bitmap";

    private ImageView imageView;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private float[] lastEvent = null;

    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;
    private float minScale = 0.5f;
    private float maxScale = 5.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        imageView = findViewById(R.id.imageView);
        ImageView btnClose = findViewById(R.id.btnClose);

        // Get the bitmap from intent
        Bitmap bitmap = getIntent().getParcelableExtra(EXTRA_IMAGE_BITMAP);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }

        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        // Setup scale gesture detector for pinch zoom
        scaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        // Setup touch listener
        imageView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);

            PointF curr = new PointF(event.getX(), event.getY());

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(curr);
                    mode = DRAG;
                    lastEvent = null;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    lastEvent = new float[4];
                    lastEvent[0] = event.getX(0);
                    lastEvent[1] = event.getX(1);
                    lastEvent[2] = event.getY(0);
                    lastEvent[3] = event.getY(1);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    lastEvent = null;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        float dx = curr.x - start.x;
                        float dy = curr.y - start.y;
                        matrix.postTranslate(dx, dy);
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    break;
            }

            imageView.setImageMatrix(matrix);
            return true;
        });

        btnClose.setOnClickListener(v -> finish());
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));

            matrix.setScale(scaleFactor, scaleFactor);
            imageView.setImageMatrix(matrix);
            return true;
        }
    }
}


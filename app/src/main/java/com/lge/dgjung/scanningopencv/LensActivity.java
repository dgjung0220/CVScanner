package com.lge.dgjung.scanningopencv;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LensActivity extends AppCompatActivity {

    private static String TAG = "LENSJDG";
    private static final int CLICK_PHOTO = 1111;
    private static final int LOAD_PHOTO = 1112;
    private static final String FILE_LOCATION = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + "Camera";
    private static final String photoPath_debugging = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "debugging_drawing"+ ".png";

    private ImageView ivImage, ivImage1, ivImage2;

    private String errorMsg;
    private Uri fileUri;

    private Mat src;
    private Mat srcOrig;
    static int scaleFactor;

    ArrayList<Point> cornersCopy = new ArrayList<>();
    ArrayList<Bitmap> bitmaplist = new ArrayList<>();
    Bitmap bitmap;

    @Override
    public void onResume() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        super.onResume();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    System.loadLibrary("opencv_java3");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lens);

        int PERMISSIONS_REQUEST_CODE = 1000;
        String[] PERMISSIONS = {"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
        if (!UserUtil.hasPermissions(this, PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        ivImage = (ImageView) findViewById(R.id.ivImage);
        ivImage1 = (ImageView) findViewById(R.id.ivImage1);
        ivImage2 = (ImageView) findViewById(R.id.ivImage2);

        Button bClickImage, bLoadImage;

        bClickImage = (Button)findViewById(R.id.bClickImage);
        bLoadImage = (Button)findViewById(R.id.bLoadImage);

        bClickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                errorMsg = null;

                File imagesFolder = new File(FILE_LOCATION);
                imagesFolder.mkdirs();

                File image = new File(imagesFolder, "image_10.jpg");
                fileUri = FileProvider.getUriForFile(getApplicationContext(), "com.lge.dgjung.fileprovider", image);
                Log.d("LensJDG", "File URI = " + fileUri.toString());
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                startActivityForResult(intent, CLICK_PHOTO);
            }
        });

        bLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                errorMsg = null;
                startActivityForResult(intent, LOAD_PHOTO);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Log.d(TAG, requestCode + " " + CLICK_PHOTO + resultCode + " " + RESULT_OK);

        switch(requestCode) {
            case CLICK_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        final InputStream imageStream = getContentResolver().openInputStream(fileUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        srcOrig = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC4);

                        src = new Mat();
                        Utils.bitmapToMat(selectedImage, srcOrig);
                        scaleFactor = calcScaleFactor(srcOrig.rows(), srcOrig.cols());

                        Imgproc.resize(srcOrig, src, new Size(srcOrig.rows()/scaleFactor,srcOrig.cols()/scaleFactor));
                        getPage();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case LOAD_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        InputStream stream = getContentResolver().openInputStream(imageReturnedIntent.getData());
                        final Bitmap selectedImage = BitmapFactory.decodeStream(stream);
                        stream.close();

                        srcOrig = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC4);
                        Imgproc.cvtColor(srcOrig, srcOrig, Imgproc.COLOR_BGR2RGB);
                        Utils.bitmapToMat(selectedImage, srcOrig);
                        scaleFactor = calcScaleFactor(srcOrig.rows(), srcOrig.cols());

                        src = new Mat();
                        Imgproc.resize(srcOrig, src, new Size(srcOrig.rows()/scaleFactor, srcOrig.cols()/scaleFactor));
                        Imgproc.GaussianBlur(src, src, new Size(5,5), 1);

                        getPage();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
    private static int calcScaleFactor(int rows, int cols){
        int idealRow, idealCol;
        if(rows<cols){
            idealRow = 240;
            idealCol = 320;
        } else {
            idealCol = 240;
            idealRow = 320;
        }

        int val = Math.min(rows / idealRow, cols / idealCol);
        if(val<=0){
            return 1;
        } else {
            return val;
        }
    }

    static double calcWhiteDist(double r, double g, double b){
        return Math.sqrt(Math.pow(255 - r, 2) +
                Math.pow(255 - g, 2) + Math.pow(255 - b, 2));
    }

    static Point findIntersection(double[] line1, double[] line2) {
        double start_x1 = line1[0], start_y1 = line1[1], end_x1 = line1[2], end_y1 = line1[3],
                start_x2 = line2[0], start_y2 = line2[1], end_x2 = line2[2], end_y2 = line2[3];

        double denominator = ((start_x1 - end_x1) * (start_y2 - end_y2)) - ((start_y1 - end_y1) * (start_x2 - end_x2));

        if (denominator!=0) {
            Point pt = new Point();
            pt.x = (int) (((start_x1 * end_y1 - start_y1 * end_x1) * (start_x2 - end_x2) - (start_x1 - end_x1) * (start_x2 * end_y2 - start_y2 * end_x2)) / denominator);
            pt.y = (int) (((start_x1 * end_y1 - start_y1 * end_x1) * (start_y2 - end_y2) - (start_y1 - end_y1) * (start_x2 * end_y2 - start_y2 * end_x2)) / denominator);
            return pt;
        }
        else
            return new Point(-1, -1);
    }

    static boolean exists(List<org.opencv.core.Point> corners, Point pt){
        for(int i=0; i<corners.size(); i++){
            if(Math.sqrt(Math.pow(corners.get(i).x-pt.x,
                    2)+Math.pow(corners.get(i).y-pt.y, 2)) < 10){
                return true;
            }
        }
        return false;
    }

    static void sortCorners(ArrayList<Point> corners)
    {
        ArrayList<Point> top, bottom;
        top = new ArrayList<Point>();
        bottom = new ArrayList<Point>();
        Point center = new Point();
        for(int i=0; i<corners.size(); i++){
            center.x += corners.get(i).x/corners.size();
            center.y += corners.get(i).y/corners.size();
        }
        for (int i = 0; i < corners.size(); i++)
        {
            if (corners.get(i).y < center.y)
                top.add(corners.get(i));
            else
                bottom.add(corners.get(i));
        }

        corners.clear();

        if (top.size() == 2 && bottom.size() == 2){
            Point top_left = top.get(0).x > top.get(1).x ? top.get(1) : top.get(0);
            Point top_right = top.get(0).x > top.get(1).x ? top.get(0) : top.get(1);
            Point bottom_left = bottom.get(0).x > bottom.get(1).x ? bottom.get(1) : bottom.get(0);
            Point bottom_right = bottom.get(0).x > bottom.get(1).x ? bottom.get(0) : bottom.get(1);
            top_left.x *= scaleFactor;
            top_left.y *= scaleFactor;
            top_right.x *= scaleFactor;
            top_right.y *= scaleFactor;
            bottom_left.x *= scaleFactor;
            bottom_left.y *= scaleFactor;
            bottom_right.x *= scaleFactor;
            bottom_right.y *= scaleFactor;
            corners.add(top_left);
            corners.add(top_right);
            corners.add(bottom_right);
            corners.add(bottom_left);
        }
    }

    private void getPage() {

        new AsyncTask<Void, Void, ArrayList<Bitmap>>() {
            ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = ProgressDialog.show(LensActivity.this, "Grabbing page", "Please Wait");
            }

            @Override
            protected ArrayList<Bitmap> doInBackground(Void... voids) {
                Mat srcRes = new Mat( src.size(), src.type() );
                Mat srcGray = new Mat();

                Mat samples = new Mat(src.rows() * src.cols(), 3, CvType.CV_32F);
                for( int y = 0; y < src.rows(); y++ ) {
                    for( int x = 0; x < src.cols(); x++ ) {
                        for( int z = 0; z < 3; z++) {
                            samples.put(x + y*src.cols(), z, src.get(y,x)[z]);
                        }
                    }
                }

                int clusterCount = 2;
                Mat labels = new Mat();
                int attempts = 5;
                Mat centers = new Mat();
                Core.kmeans(samples, clusterCount, labels, new TermCriteria(TermCriteria.MAX_ITER | TermCriteria.EPS, 10000, 0.0001), attempts, Core.KMEANS_PP_CENTERS, centers);

                double dstCenter0 = calcWhiteDist(centers.get(0, 0)[0], centers.get(0, 1)[0], centers.get(0, 2)[0]);
                double dstCenter1 = calcWhiteDist(centers.get(1, 0)[0], centers.get(1, 1)[0], centers.get(1, 2)[0]);
                int paperCluster = (dstCenter0 < dstCenter1)?0:1;

                for( int y = 0; y < src.rows(); y++ ) {
                    for( int x = 0; x < src.cols(); x++ )
                    {
                        int cluster_idx = (int)labels.get(x + y*src.cols(),0)[0];
                        if(cluster_idx != paperCluster){
                            srcRes.put(y,x, 0, 0, 0, 255);
                        } else {
                            srcRes.put(y,x, 255, 255, 255, 255);
                        }
                    }
                }

                Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
                Imgproc.Canny(srcGray, srcGray, 50, 150);
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                Mat hierarchy = new Mat();

                Mat drawing = Mat.zeros(srcRes.size(), CvType.CV_8UC1);
                Imgproc.findContours(srcGray, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

                MatOfPoint approxContour = new MatOfPoint();
                MatOfPoint2f thisContour2f = new MatOfPoint2f();
                MatOfPoint2f approxContour2f = new MatOfPoint2f();
                List<Point> foundCnt = new ArrayList<>();

                int index = 0;
                for (MatOfPoint c : contours) {
                    c.convertTo(thisContour2f, CvType.CV_32FC2);

                    double peri = Imgproc.arcLength(thisContour2f, true);
                    Imgproc.approxPolyDP(thisContour2f, approxContour2f, 0.02 * peri, true);
                    approxContour2f.convertTo(approxContour, CvType.CV_32S);

                    if (approxContour.size().height == 4) {
                        for (Point p : approxContour.toArray()) {
                           foundCnt.add(p);
                        }
                        break;
                    }
                    index++;
                }

                Imgproc.drawContours(src, contours, index, new Scalar(255), 10);
                Imgproc.drawContours(drawing, contours, index, new Scalar(255), 10);

                bitmap = Bitmap.createBitmap(drawing.cols(), drawing.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(drawing, bitmap);
                bitmaplist.add(bitmap);

                bitmap = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(src, bitmap);
                bitmaplist.add(bitmap);

                ArrayList<Point> corners = new ArrayList<>();
                for (Point p : foundCnt) {
                    Log.d(TAG, p.x + "," + p.y);
                    corners.add(new Point(p.x,p.y));
                }

                sortCorners(corners);

                double top = Math.sqrt(Math.pow(corners.get(0).x - corners.get(1).x, 2) + Math.pow(corners.get(0).y - corners.get(1).y, 2));
                double right = Math.sqrt(Math.pow(corners.get(1).x - corners.get(2).x, 2) + Math.pow(corners.get(1).y - corners.get(2).y, 2));
                double bottom = Math.sqrt(Math.pow(corners.get(2).x - corners.get(3).x, 2) + Math.pow(corners.get(2).y - corners.get(3).y, 2));
                double left = Math.sqrt(Math.pow(corners.get(3).x - corners.get(1).x, 2) + Math.pow(corners.get(3).y - corners.get(1).y, 2));

                Mat quad = Mat.zeros(new Size(Math.max(top, bottom), Math.max(left, right)), CvType.CV_8UC3);

                ArrayList<Point> result_pts = new ArrayList<>();
                result_pts.add(new Point(0, 0));
                result_pts.add(new Point(quad.cols()-1, 0));
                result_pts.add(new Point(quad.cols()-1, quad.rows()-1));
                result_pts.add(new Point(0, quad.rows()-1));

                Mat cornerPts = Converters.vector_Point2f_to_Mat(corners);
                Mat resultPts = Converters.vector_Point2f_to_Mat(result_pts);

                Mat transformation = Imgproc.getPerspectiveTransform(cornerPts, resultPts);
                Imgproc.warpPerspective(src, quad, transformation, quad.size());

                Imgproc.cvtColor(quad, quad, Imgproc.COLOR_BGR2RGBA);

                bitmap = Bitmap.createBitmap(quad.cols(), quad.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(quad, bitmap);
                bitmaplist.add(bitmap);

                return bitmaplist;
            }

            @Override
            protected void onPostExecute(ArrayList<Bitmap> bitmap) {
                super.onPostExecute(bitmap);
                dialog.dismiss();

                if(bitmap!=null) {
                    ivImage.setImageBitmap(bitmap.get(0));
                    ivImage1.setImageBitmap(bitmap.get(1));
                    ivImage2.setImageBitmap(bitmap.get(2));
                } else if (errorMsg != null){
                    Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void onTakePhotoFailed() {
        final String errorMessage = "실패";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LensActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

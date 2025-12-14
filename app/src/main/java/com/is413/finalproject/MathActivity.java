package com.is413.finalproject;

import android.content.Intent;
import android.os.Bundle;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.nex3z.fingerpaintview.FingerPaintView;

import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class MathActivity extends AppCompatActivity {

    //new variables for MNIST
    private Interpreter interpreter; //For interpreting the TFLite model
    private static final int IMAGE_WIDTH = 28; //MNIST image width
    private static final int IMAGE_HEIGHT = 28; //MNIST image height
    private static final int NUM_CLASSES = 10; //digits 0-9

    // establish variables
    private int pts = 0;
    private int correctAns = 0;
    private int userAns;
    private String msg;
    private TextView score;
    private TextView q;
    private TextView userInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math);

        // finding views by id
        score = findViewById(R.id.scoreTV);
        q = findViewById(R.id.question);
        userInput = findViewById(R.id.ans);

        // randomize the question and set score to 0 on first enter
        genQ();

        // load the MNIST model
        try{
            interpreter = new Interpreter(loadModelFile("digit.tflite"));
        }catch (IOException e){
            throw new RuntimeException(e);
        }

        // set up Buttons
        Button btn_submit = findViewById(R.id.submit);
        Button btn_input = findViewById(R.id.input);
        Button btn_clear = findViewById(R.id.clear);

        // set up Finger Paint stuff
        FingerPaintView fpv = findViewById(R.id.fpv_paint);
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(24);
        p.setColor(Color.BLACK);
        fpv.setPen(p);

        // submit answer button onClickListener
        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // clears the fingerpaint board
                fpv.clear();
                userAns = Integer.valueOf(userInput.getText().toString());

                // checks if the player got the correct answer
                if (correctAns == userAns) {
                    pts += 1;
                    msg = "You got the correct answer!";
                    Toast.makeText(MathActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    if (pts > 0) {
                        pts -= 1;
                    }
                    msg = "The correct answer was " + correctAns;
                    Toast.makeText(MathActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
                score.setText("Score: " + pts);

                // checks if player has 10 points or not
                if (pts >= 10) {
                    Intent intent = new Intent(MathActivity.this, WinActivity.class);
                    startActivity(intent);
                } else {
                    genQ();
                    userInput.setText("0");
                }
            }
        });

        // input number button onClickListener
        btn_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //FingerPaintView bitmap
                Bitmap bitmap = fpv.exportToBitmap(IMAGE_WIDTH, IMAGE_HEIGHT);

                //Convert the bitmap to ByteBuffer to use TFLite
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(IMAGE_WIDTH * IMAGE_HEIGHT * 4);
                byteBuffer.order(ByteOrder.nativeOrder());
                int[] pixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
                bitmap.getPixels(pixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
                for (int pixel : pixels){
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    float gray = (r+g+b)/ 3.0f;
                    gray = 255 - gray;
                    float grayscale = gray/255.0f;
                    byteBuffer.putFloat(grayscale);
                }

                //Run inference
                float[][] result = new float[1][NUM_CLASSES];
                interpreter.run(byteBuffer, result);

                //calculates the predicted digit using what number has the highest probability recognition
                int predictedDigit = -1;
                float maxProb = -1.0f;
                for (int i = 0; i < NUM_CLASSES; i++){
                    if(result[0][i] > maxProb){
                        maxProb = result[0][i];
                        predictedDigit = i;
                    }
                }

                // predicted digit set to user input
                String currentInput = userInput.getText().toString();
                // clear the 0 to prevent 0 in front of a number
                if (currentInput.equals("0")){
                    currentInput = "";
                }

                // add a second digit to the right of the first
                // appends another digit to the right of the most recent input
                currentInput += predictedDigit;
                userInput.setText(currentInput);
                fpv.clear();
            }
        });

        // clear answer button onClickListener
        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // clear the board and the user input
                fpv.clear();
                userInput.setText("0");
            }
        });
    }

    // method to generate a new question
    private void genQ() {
        // declaring string to hold the equation
        String qText;
        // generates 2 random numbers from 1 to 20
        int rng1 = (int)(Math.random() * 20) + 1;
        int rng2 = (int)(Math.random() * 20) + 1;

        // generates a number 0 or 1 to decide the operation
        int op = (int)(Math.random() * 2);
        String opSymbol;

        // sets the operation in the equation and gets the correct answer
        if (op == 0) {
            correctAns = rng1 + rng2;
            opSymbol = "+";
        } else {
            correctAns = rng1 * rng2;
            opSymbol = "*";
        }

        // changes the text to show the full equation
        qText = rng1 + " " + opSymbol + " " + rng2 + " = ";
        q.setText(qText);
    }

    // method for loading the model file
    private MappedByteBuffer loadModelFile(String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long length = fileDescriptor.getLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length);
    }
}
package com.deskode.recorddialog;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;

public class RecordDialog extends DialogFragment {
    static final String TAG = "RecordDialog";
    private String _strTitle;
    private String _strMessage;
    private String _strPositiveButtonText;
    private String _strNegativeButtonText;
    private FloatingActionButton _recordButton;
    private String STATE_BUTTON = "INIT";
    private String _AudioSavePathInDevice = null;
    private TextView _timerView;
    private Timer _timer;
    private int recorderSecondsElapsed;
    private int playerSecondsElapsed;

    private ClickListener _clickListener;
    private NegativeClickListener _negativeClickListener;
    Recorder recorder;
    MediaPlayer mediaPlayer;
    MediaPlayer mPlayer;

    boolean isRecording = false;

    AudioRecord audioRecord;
    AndroidLame androidLame;
    FileOutputStream outputStream;
    int minBuffer;
    int inSamplerate = 8000;

    public RecordDialog() {

    }

    public static RecordDialog newInstance(String title) {
        RecordDialog frag = new RecordDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams
                .SOFT_INPUT_STATE_HIDDEN);
        setupRecorder();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Getting the layout inflater to inflate the view in an alert dialog.
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View rootView = inflater.inflate(R.layout.record_dialog, null);
        String strMessage = _strMessage == null ? "Presiona para grabar" : _strMessage;
        _timerView = rootView.findViewById(R.id._txtTimer);
        _timerView.setText(strMessage);
        _recordButton = rootView.findViewById(R.id.btnRecord);
        _recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scaleAnimation();
                Context context= null;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    context = getContext();
                } else{
                    context = getActivity();

                }
                switch (STATE_BUTTON) {
                    case "INIT":
                        _recordButton.setImageResource(R.drawable.ic_stop);
                        STATE_BUTTON = "RECORD";
                        if (!isRecording) {
                            mPlayer = MediaPlayer.create(context, R.raw.hangouts_message);
                            mPlayer.start();
                            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            startTimer();
                                            isRecording = true;
                                            startRecording();
                                        }
                                    }.start();
                                }
                            });

                        }else
                            Toast.makeText(context, getString(R.string.already_recording), Toast.LENGTH_SHORT).show();
                        /*try {
                            mPlayer = MediaPlayer.create(context, R.raw.hangouts_message);
                            mPlayer.start();
                            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    recorder.startRecording();
                                    startTimer();
                                }
                            });
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "Record error ");
                            e.printStackTrace();
                        }*/
                        break;
                    case "RECORD":
                        /*try {
                            recorder.stopRecording();
                            mPlayer = MediaPlayer.create(context, R.raw.pop);
                            mPlayer.start();
                        } catch (IOException e) {
                            Log.e(TAG, "RECORD  error ");
                            e.printStackTrace();
                        }*/
                        mPlayer = MediaPlayer.create(context, R.raw.pop);
                        mPlayer.start();
                        isRecording = false;
                        _recordButton.setImageResource(R.drawable.ic_play);
                        STATE_BUTTON = "STOP";
                        _timerView.setText("00:00:00");
                        recorderSecondsElapsed = 0;
                        break;
                    case "STOP":
                        startMediaPlayer();
                        break;
                    case "PLAY":
                        pauseMediaPlayer();
                        break;
                    case "PAUSE":
                        resumeMediaPlayer();
                        break;
                }
            }
        });

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setView(rootView);

        String strPositiveButton = _strPositiveButtonText == null ? "CLOSE" : _strPositiveButtonText;
        String strNegativeButton = _strNegativeButtonText == null ? "CLOSE" : _strNegativeButtonText;
        alertDialog.setPositiveButton(strPositiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (STATE_BUTTON.equals("RECORD")) {
                    try {
                        /*recorder.stopRecording();*/
                        stopTimer();
                        isRecording = false;
                    } catch (/*IO*/Exception e) {
                        e.printStackTrace();
                    }
                }
                stopMediaPlayer();
                _clickListener.OnClickListener(_AudioSavePathInDevice);
            }
        }).setNegativeButton(strNegativeButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _negativeClickListener.OnNegativeClick(dialog);
            }
        });

        String strTitle = _strTitle == null ? "Record audio" : _strTitle;
        alertDialog.setTitle(strTitle);

        recorderSecondsElapsed = 0;
        playerSecondsElapsed = 0;

        final AlertDialog dialog = alertDialog.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams
                .SOFT_INPUT_STATE_HIDDEN);

        return dialog;
    }

    private void startRecording() {

        minBuffer = AudioRecord.getMinBufferSize(inSamplerate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC, inSamplerate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBuffer * 2);

        //5 seconds data
        short[] buffer = new short[inSamplerate * 2 * 5];

        // 'mp3buf' should be at least 7200 bytes long
        // to hold all possible emitted data.
        byte[] mp3buffer = new byte[(int) (7200 + buffer.length * 2 * 1.25)];

        try {
            outputStream = new FileOutputStream(file());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        androidLame = new LameBuilder()
                .setInSampleRate(inSamplerate)
                .setOutChannels(1)
                .setOutBitrate(32)
                .setOutSampleRate(inSamplerate)
                .build();

        audioRecord.startRecording();

        int bytesRead = 0;

        while (isRecording) {

            bytesRead = audioRecord.read(buffer, 0, minBuffer);

            if (bytesRead > 0) {

                int bytesEncoded = androidLame.encode(buffer, buffer, bytesRead, mp3buffer);

                if (bytesEncoded > 0) {
                    try {
                        outputStream.write(mp3buffer, 0, bytesEncoded);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        int outputMp3buf = androidLame.flush(mp3buffer);

        if (outputMp3buf > 0) {
            try {
                outputStream.write(mp3buffer, 0, outputMp3buf);
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        audioRecord.stop();
        audioRecord.release();

        androidLame.close();

        isRecording = false;
    }

    // Change End

    public void setTitle(String strTitle) {
        _strTitle = strTitle;
    }

    public void setMessage(String strMessage) {
        _strMessage = strMessage;
    }

    public void setPositiveButton(String strPositiveButtonText, ClickListener onClickListener) {
        _strPositiveButtonText = strPositiveButtonText;
        _clickListener = onClickListener;
    }

    public void setNegativeButton(String strNegativeButtonText, NegativeClickListener onClickListener){
        _strNegativeButtonText = strNegativeButtonText;
        _negativeClickListener = onClickListener;
    }

    private void setupRecorder() {
        recorder = OmRecorder.wav(
                new PullTransport.Default(mic(), new PullTransport.OnAudioChunkPulledListener() {
                    @Override
                    public void onAudioChunkPulled(AudioChunk audioChunk) {
                    }
                }), file());
    }

    private PullableSource mic() {
        return new PullableSource.Default(
                new AudioRecordConfig.Default(
                        MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.CHANNEL_IN_MONO, 44100
                )
        );
    }

    @NonNull
    private File file() {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File myDir = null;
        //File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), timeStamp + ".wav");
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + "blujobspro");
        } else{
            myDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" + "blujobspro");
        }

        if(!myDir.exists()){
            myDir.mkdirs();
        }

        File file = new File(myDir.getAbsolutePath(), timeStamp + ".mp3");
        _AudioSavePathInDevice = file.getPath();
        return file;
    }

    public String getAudioPath() {
        return _AudioSavePathInDevice;
    }

    private void startMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(_AudioSavePathInDevice);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopMediaPlayer();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        _recordButton.setImageResource(R.drawable.ic_pause);
        STATE_BUTTON = "PLAY";
        playerSecondsElapsed = 0;
        startTimer();
        mediaPlayer.start();
    }

    private void resumeMediaPlayer() {
        _recordButton.setImageResource(R.drawable.ic_pause);
        STATE_BUTTON = "PLAY";
        mediaPlayer.start();
    }

    private void pauseMediaPlayer() {
        _recordButton.setImageResource(R.drawable.ic_play);
        STATE_BUTTON = "PAUSE";
        mediaPlayer.pause();
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
            _recordButton.setImageResource(R.drawable.ic_play);
            STATE_BUTTON = "STOP";
            _timerView.setText("00:00:00");
            stopTimer();
        }
    }

    private void startTimer() {
        stopTimer();
        _timer = new Timer();
        _timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (_timer != null) {
            _timer.cancel();
            _timer.purge();
            _timer = null;
        }
    }

    private void updateTimer() {
        // here you check the value of getActivity() and break up if needed
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (STATE_BUTTON.equals("RECORD")) {
                    recorderSecondsElapsed++;
                    _timerView.setText(Util.formatSeconds(recorderSecondsElapsed));
                    if (recorderSecondsElapsed > 179){
                        mPlayer = MediaPlayer.create(getActivity(), R.raw.pop);
                        mPlayer.start();
                        isRecording = false;
                        _recordButton.setImageResource(R.drawable.ic_play);
                        STATE_BUTTON = "STOP";
                        _timerView.setText("00:00:00");
                        recorderSecondsElapsed = 0;
                        Toast.makeText(getActivity(), getString(R.string.warning_audio_length_limit), Toast.LENGTH_SHORT).show();
                    }
                } else if (STATE_BUTTON.equals("PLAY")) {
                    playerSecondsElapsed++;
                    _timerView.setText(Util.formatSeconds(playerSecondsElapsed));
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void scaleAnimation() {
        Context context= null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            context = getContext();
        } else{
            context = getActivity();

        }
        final Interpolator interpolador = AnimationUtils.loadInterpolator(context,
                android.R.interpolator.fast_out_slow_in);
        _recordButton.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setInterpolator(interpolador)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        _recordButton.animate().scaleX(1f).scaleY(1f).start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    public interface ClickListener {
        void OnClickListener(String path);
    }

    public interface NegativeClickListener{
        void OnNegativeClick(DialogInterface dialog);
    }
}

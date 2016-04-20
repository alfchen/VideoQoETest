/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.umich.alfred.videoqoetest;

import edu.umich.alfred.videoqoetest.player.DemoPlayer;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.util.VerboseLogUtil;

import android.content.Intent;
import android.media.MediaCodec.CryptoException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Logs player events using {@link Log}.
 */
public class EventLogger implements DemoPlayer.Listener, DemoPlayer.InfoListener,
    DemoPlayer.InternalErrorListener {
  
  public static HashMap<Pair<Integer, Integer>, Integer> Resolution2Bitrate = new HashMap<Pair<Integer, Integer>, Integer>();
  public static HashMap<String, Integer> Id2Bitrate = new HashMap<String, Integer>();
  
  private ArrayList<String> dropFrameTime;
  private ArrayList<Pair<String, Integer>> videoBitrateVarience;
  private ArrayList<Pair<String, Integer>> audioBitrateVarience;
  private double initialLoadingTime;
  private ArrayList<Double> rebufferTime;
  private ArrayList<Pair<String, Double>> videoGoodput;
  private ArrayList<Long> videoGoodputEstimate;
  private ArrayList<Pair<String, Double>> audioGoodput;
  private int previousVideoBitrate;
  private int previousAudioBitrate;
  private long switchToSteadyStateTime;
  private long totalBytesDownloaded;
  
  private double initialLoadingTime_s;
  private int bufferCounter = 0;
  private double bufferTime_s;
  
  private static final String TAG = "EventLogger";
  private static final NumberFormat TIME_FORMAT;
  static {
    TIME_FORMAT = NumberFormat.getInstance(Locale.US);
    TIME_FORMAT.setMinimumFractionDigits(4);
    TIME_FORMAT.setMaximumFractionDigits(4);
  }

  private long sessionStartTimeMs;
  private long[] loadStartTimeMs;
  private long[] availableRangeValuesUs;

  public EventLogger() {
    loadStartTimeMs = new long[DemoPlayer.RENDERER_COUNT];
    
    this.dropFrameTime = new ArrayList<String>();
    this.videoBitrateVarience = new ArrayList<Pair<String, Integer>>();
    this.audioBitrateVarience = new ArrayList<Pair<String, Integer>>();
    this.rebufferTime = new ArrayList<Double>();
    this.videoGoodput = new ArrayList<Pair<String, Double>>();
    this.videoGoodputEstimate = new ArrayList<Long>();
    this.audioGoodput = new ArrayList<Pair<String, Double>>();
    this.videoBitrateVarience.add(Pair.create("0.00", 0));
    this.previousVideoBitrate = 0;
    this.audioBitrateVarience.add(Pair.create("0.00", 0));
    this.previousAudioBitrate = 0;
    this.switchToSteadyStateTime = -1;
    this.totalBytesDownloaded = 0;
  }
  
  public void startSession() {
    sessionStartTimeMs = SystemClock.elapsedRealtime();
    String rep = "start\t0";
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
  }

  public void endSession() {
    String rep = "end\t" + getSessionTimeString();
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
    this.videoBitrateVarience.add(Pair.create(getSessionTimeString(), this.previousVideoBitrate));
    this.audioBitrateVarience.add(Pair.create(getSessionTimeString(), this.previousAudioBitrate));
    printStatInfo();
  }

  // DemoPlayer.Listener

  @Override
  public void onStateChanged(boolean playWhenReady, int state) {
    String rep = "state\t" + getSessionTimeString() + "\t" + playWhenReady + "\t" +
            getStateString(state);
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);

//    DecimalFormat df = new DecimalFormat("#.##");
    switch(state) {
      case ExoPlayer.STATE_PREPARING:
//        this.bitrateVarience.add(Pair.create("0.00", currentBitrate));
        this.initialLoadingTime_s = Double.parseDouble(getSessionTimeString());
        break;

      case ExoPlayer.STATE_BUFFERING:
        bufferCounter++;
        bufferTime_s = Double.parseDouble(getSessionTimeString());
        break;
      case ExoPlayer.STATE_READY:
        if (bufferCounter == 1) {
          this.initialLoadingTime = Double.parseDouble(String.format("%.2f", Double.parseDouble(getSessionTimeString()) - this.initialLoadingTime_s));
        }
        else {
          this.rebufferTime.add(Double.parseDouble(String.format("%.2f", Double.parseDouble(getSessionTimeString()) - bufferTime_s)));
        }
        break;
      case ExoPlayer.STATE_ENDED:
        this.videoBitrateVarience.add(Pair.create(getSessionTimeString(), this.previousVideoBitrate));
        this.audioBitrateVarience.add(Pair.create(getSessionTimeString(), this.previousAudioBitrate));
//        printStatInfo();
        break;
    }
  }

  private int printStatInfo() {
//    Log.e("", "DropFrame #: " + this.dropFrameTime.size());
//    Log.e("", "Bitrate: " + displayBitrate(this.bitrateVarience));
//    Log.e("", "Initial Loading Time: " + this.initialLoadingTime);
//    Log.e("", "Rebuffering: " + this.rebufferTime);
    Log.e("ashkan_video", "" + this.dropFrameTime.size());
    //Log.e("ashkan_video", "" + displayGoodPut(this.videoGoodput));
    Log.e("ashkan_video", "" + displayBitrate(this.videoBitrateVarience));
    Log.e("ashkan_video", "" + this.initialLoadingTime);
    Log.e("ashkan_video", "" + this.rebufferTime);
    //Log.e("ashkan_video", "" + displayGoodPut(this.audioGoodput));
    Log.e("ashkan_video", "" + displayBitrate(this.audioBitrateVarience));
    Log.e("ashkan_video", "bba switch time " + this.switchToSteadyStateTime);
    Log.e("ashkan_video", "total bytes downloaded " + this.totalBytesDownloaded);
    
    return 0;
  }

  
  private String displayBitrate(ArrayList<Pair<String, Integer>> bitrateVarience2) {
    StringBuilder sBuilder = new StringBuilder();
    for (Pair<String, Integer> pBitrate : bitrateVarience2) {
      sBuilder.append(pBitrate.first + " " + pBitrate.second + "\n");
    }
    return sBuilder.toString();
  }

  private String displayGoodPut(ArrayList<Pair<String, Double>> goodput) {
    StringBuilder sBuilder = new StringBuilder();
    for (Pair<String, Double> pBitrate : goodput) {
      sBuilder.append(pBitrate.first + " " + String.format("%.2f", pBitrate.second) + "\n");
    }
    return sBuilder.toString();
  }
  
  // Error finished, return partial results
  @Override
  public void onError(Exception e) {
    Log.e(TAG, "playerFailed [" + getSessionTimeString() + "]", e);
    String rep = "playerFailed\t" + getSessionTimeString();

    MainActivity.report(rep, MainActivity.resultlogfilename);
//    this.videoBitrateVarience.add(Pair.create(getSessionTimeString(), this.previousVideoBitrate));
//    this.audioBitrateVarience.add(Pair.create(getSessionTimeString(), this.previousAudioBitrate));
//    printStatInfo();
  }

  @Override
  public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                 float pixelWidthHeightRatio) {
    String rep = "videoSizeChanged\t" + width + "\t" + height + "\t" + unappliedRotationDegrees
            + "\t" + pixelWidthHeightRatio;
    Log.d(TAG, rep);

    MainActivity.report(rep, MainActivity.resultlogfilename);
  }

  // DemoPlayer.InfoListener

  @Override
  public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
    String rep = "bandwidth\t" + getSessionTimeString() + "\t" + bytes + "\t"
            + getTimeString(elapsedMs) + "\t" + bitrateEstimate;
    Log.d(TAG, rep);

    MainActivity.report(rep, MainActivity.resultlogfilename);
  }

  /*
  @Override
//  public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
  public void onBandwidthSample(String label, long startTime, long endTime, int elapsedMs, long bytes, long bitrateEstimate) {
    Log.d(TAG, label + " bandwidth [" + startTime + ", " + endTime + ", " + getSessionTimeString() + ", " + bytes +
            ", " + getTimeString(elapsedMs) + ", " + bitrateEstimate + ", " + bytes * 8 / elapsedMs + "kbps]");
    if (label.equals("video")) {
      this.videoGoodput.add(Pair.create(getSessionTimeString(), (double)bytes * 8000 / elapsedMs));
      this.videoGoodputEstimate.add(bitrateEstimate);
    }
    else if (label.equals("audio")) {
      this.audioGoodput.add(Pair.create(getSessionTimeString(), (double)bytes * 8000 / elapsedMs));
    }
//    this.goodput.add(Pair.create(getSessionTimeString(), (double)bytes * 8000 / elapsedMs));
  }
*/

  @Override
  public void onDroppedFrames(int count, long elapsed) {
    String rep = "droppedFrames\t" + getSessionTimeString() + "\t" + count;
    Log.d(TAG, rep);

    MainActivity.report(rep, MainActivity.resultlogfilename);

    this.dropFrameTime.add(getSessionTimeString());
  }

  @Override
  public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                            long mediaStartTimeMs, long mediaEndTimeMs) {
    loadStartTimeMs[sourceId] = SystemClock.elapsedRealtime();
    if (VerboseLogUtil.isTagEnabled(TAG)) {
      String rep = "loadStart\t" + getSessionTimeString() + "\t" + sourceId
              + "\t" + mediaStartTimeMs + "\t" + mediaEndTimeMs;
      Log.d(TAG, rep);

      MainActivity.report(rep, MainActivity.resultlogfilename);
    }
  }

  @Override
  public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                              long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
    if (VerboseLogUtil.isTagEnabled(TAG)) {
      long downloadTime = SystemClock.elapsedRealtime() - loadStartTimeMs[sourceId];
      String rep = "loadEnd\t" + getSessionTimeString() + "\t" + sourceId + "\t" + downloadTime;
      Log.d(TAG, rep);

      MainActivity.report(rep, MainActivity.resultlogfilename);
    }
  }

  @Override
  public void onVideoFormatEnabled(Format format, int trigger, long mediaTimeMs) {
    int currentBitrate = Id2Bitrate.get(format.id);
    String rep = "videoFormat\t" + getSessionTimeString() + "\t" + format.id + "\t"
            + Integer.toString(trigger);
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);

    rep = "videoFormat\t"+ getSessionTimeString() + "\t" + format.id + "\t" +
            Integer.toString(trigger) + "\t" + currentBitrate;
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);

    this.videoBitrateVarience.add(Pair.create(getSessionTimeString(), this.previousVideoBitrate));
    rep = "videoBitratePrevious\t" + getSessionTimeString() +"\t"+this.previousVideoBitrate;
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
    this.videoBitrateVarience.add(Pair.create(getSessionTimeString(), currentBitrate));
    rep = "videoBitrateCurrent\t" + getSessionTimeString() + "\t" + currentBitrate;
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
    this.previousVideoBitrate = currentBitrate;
  }

  @Override
  public void onAudioFormatEnabled(Format format, int trigger, long mediaTimeMs) {
    int currentBitrate = Id2Bitrate.get(format.id);
    String rep = "audioFormat\t" + getSessionTimeString() + "\t" + format.id + "\t" +
            Integer.toString(trigger) + "\t"+ currentBitrate;
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
    this.audioBitrateVarience.add(Pair.create(getSessionTimeString(), this.previousAudioBitrate));
    rep = "audioBitratePrevious\t" + getSessionTimeString() + "\t" + this.previousAudioBitrate;
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
    this.audioBitrateVarience.add(Pair.create(getSessionTimeString(), currentBitrate));
    rep = "audioBitrateCurrent\t" + getSessionTimeString() + "\t" + currentBitrate;
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
    this.previousAudioBitrate = currentBitrate;
  }

  // DemoPlayer.InternalErrorListener

  @Override
  public void onLoadError(int sourceId, IOException e) {
    printInternalError("loadError", e);
  }

  @Override
  public void onRendererInitializationError(Exception e) {
    printInternalError("rendererInitError", e);
  }

  @Override
  public void onDrmSessionManagerError(Exception e) {
    printInternalError("drmSessionManagerError", e);
  }

  @Override
  public void onDecoderInitializationError(DecoderInitializationException e) {
    printInternalError("decoderInitializationError", e);
  }

  @Override
  public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
    printInternalError("audioTrackInitializationError", e);
  }

  @Override
  public void onAudioTrackWriteError(AudioTrack.WriteException e) {
    printInternalError("audioTrackWriteError", e);
  }

  @Override
  public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
    printInternalError("audioTrackUnderrun [" + bufferSize + ", " + bufferSizeMs + ", "
            + elapsedSinceLastFeedMs + "]", null);
  }

  @Override
  public void onCryptoError(CryptoException e) {
    printInternalError("cryptoError", e);
  }

  @Override
  public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                   long initializationDurationMs) {
    String rep = "decoderInitialized\t" + getSessionTimeString() + "\t" + decoderName;
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
  }


  @Override
  public void onAvailableRangeChanged(int sourceId, TimeRange availableRange) {
    availableRangeValuesUs = availableRange.getCurrentBoundsUs(availableRangeValuesUs);
    String rep = "availableRange\t" + availableRange.isStatic() + "\t" + availableRangeValuesUs[0]
            + "\t" + availableRangeValuesUs[1];
    Log.d(TAG, rep);
    MainActivity.report(rep, MainActivity.resultlogfilename);
  }

  private void printInternalError(String type, Exception e) {
    Log.e(TAG, "internalError [" + getSessionTimeString() + ", " + type + "]", e);
    String rep = "internalError\t" + getSessionTimeString() + "\t" + type;
    MainActivity.report(rep, MainActivity.resultlogfilename);
  }

  private String getStateString(int state) {
    switch (state) {
      case ExoPlayer.STATE_BUFFERING:
        return "B";
      case ExoPlayer.STATE_ENDED:
        return "E";
      case ExoPlayer.STATE_IDLE:
        return "I";
      case ExoPlayer.STATE_PREPARING:
        return "P";
      case ExoPlayer.STATE_READY:
        return "R";
      default:
        return "?";
    }
  }

  private String getSessionTimeString() {
    return getTimeString(SystemClock.elapsedRealtime() - sessionStartTimeMs);
//    return getTimeString(System.currentTimeMillis());
  }

  private String getTimeString(long timeMs) {
    return TIME_FORMAT.format((timeMs) / 1000f);
  }

}
